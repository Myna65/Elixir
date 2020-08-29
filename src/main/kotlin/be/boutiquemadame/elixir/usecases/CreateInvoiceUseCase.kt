package be.boutiquemadame.elixir.usecases

import arrow.core.Either
import be.boutiquemadame.elixir.domain.contracts.ArticleGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.*
import be.boutiquemadame.elixir.shared.UseCase
import java.lang.Exception
import java.math.BigDecimal
import java.time.LocalDate

class CreateInvoiceUseCase(
    private val articleGateway: ArticleGateway,
    private val invoiceGateway: InvoiceGateway,
    private val invoiceLineGateway: InvoiceLineGateway
) : UseCase<CreateInvoiceRequest, CreateInvoiceError, CreateInvoiceResponse> {

    override suspend fun execute(request: CreateInvoiceRequest): Either<CreateInvoiceError, CreateInvoiceResponse> {
        val invoice = createAndSaveInvoice(request)

        createAndSaveInvoiceLines(request.lines, invoice.id)

        return Either.right(CreateInvoiceResponse.fromInvoice(invoice))
    }

    private suspend fun createAndSaveInvoice(request: CreateInvoiceRequest): Invoice {
        val linesSum = sumLines(request.lines)

        val invoice = Invoice.createInvoice(request.invoiceDate, linesSum, request.vatAmount)
        invoiceGateway.save(invoice)
        return invoice
    }

    private fun sumLines(lines: List<CreateInvoiceRequestLine>): BigDecimal {
        return lines.fold(BigDecimal.ZERO) {
            acc, createInvoiceRequestLine ->
            when (createInvoiceRequestLine) {
                is CreateInvoiceRequestTextLine -> {
                    acc.add(createInvoiceRequestLine.price)
                }
                is CreateInvoiceRequestLineWithNewProduct -> {
                    acc.add(createInvoiceRequestLine.unitPrice * BigDecimal(createInvoiceRequestLine.quantity))
                }
            }
        }
    }

    private suspend fun createAndSaveInvoiceLines(
        lines: List<CreateInvoiceRequestLine>,
        invoiceId: InvoiceId
    ) {
        val invoiceLines = lines.mapIndexed {
            index, line ->
            when (line) {
                is CreateInvoiceRequestTextLine -> createInvoiceLineWithoutProduct(line, invoiceId, index)
                is CreateInvoiceRequestLineWithNewProduct -> Either.right(createInvoiceLineWithNewProduct(line, invoiceId, index))
            }
        }

        invoiceLines

        invoiceLineGateway.saveMultiple(invoiceLines)
    }

    private fun createInvoiceLineWithoutProduct(
        request: CreateInvoiceRequestTextLine,
        invoiceId: InvoiceId,
        index: Int
    ): Either<CreateInvoiceError, InvoiceLine> {
        return InvoiceTextLine.create(
            invoiceId,
            index + 1,
            request.description,
            request.price
        ).mapLeft { error -> CreateInvoiceError.NegativePrice(error) }
    }

    private suspend fun createInvoiceLineWithNewProduct(
        request: CreateInvoiceRequestLineWithNewProduct,
        invoiceId: InvoiceId,
        index: Int
    ): InvoiceLine {
        val product = Product.create(request.model, request.articleNumber, request.size, request.color)

        articleGateway.save(product)

        return InvoiceProductLine.create(
            invoiceId,
            index + 1,
            product.getId(),
            request.quantity,
            request.unitPrice
        )
    }

}

data class CreateInvoiceRequest(
    val invoiceDate: LocalDate,
    val vatAmount: BigDecimal,
    val lines: List<CreateInvoiceRequestLine>
)

sealed class CreateInvoiceRequestLine

data class CreateInvoiceRequestTextLine(
    val description: String,
    val price: BigDecimal
) : CreateInvoiceRequestLine()

data class CreateInvoiceRequestLineWithNewProduct(
    val model: String,
    val articleNumber: String,
    val size: String,
    val color: String,
    val quantity: Int,
    val unitPrice: BigDecimal
) : CreateInvoiceRequestLine()

data class CreateInvoiceResponse(
    val id: String
) {
    companion object {
        fun fromInvoice(invoice: Invoice): CreateInvoiceResponse {
            return CreateInvoiceResponse(
                invoice.id.raw
            )
        }
    }
}

sealed class CreateInvoiceError {
    data class NegativePrice(val error: NegativePriceError) : CreateInvoiceError()
}

class AlreadyExistingProduct(lineIndex: Int, model: String, articleNumber: String, size: String, color: String, foundProductId: InvoiceId) :
    Exception("The product requested for creation already exists.\nLine $lineIndex: \"$model\", \"$articleNumber\", \"$size\", \"$color\" already exists in the database under id: ${foundProductId.raw}")
