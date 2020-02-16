package be.boutiquemadame.elixir.usecases

import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.Invoice
import be.boutiquemadame.elixir.domain.entities.InvoiceId
import be.boutiquemadame.elixir.domain.entities.InvoiceLine
import be.boutiquemadame.elixir.shared.UseCase
import java.math.BigDecimal
import java.time.LocalDate

class CreateInvoiceUseCase(
    private val invoiceGateway: InvoiceGateway,
    private val invoiceLineGateway: InvoiceLineGateway
) : UseCase<CreateInvoiceRequest, CreateInvoiceResponse> {

    override suspend fun execute(request: CreateInvoiceRequest): CreateInvoiceResponse {
        val invoice = createAndSaveInvoice(request)

        createAndSaveInvoiceLines(request.lines, invoice.id)

        return CreateInvoiceResponse.fromInvoice(invoice)
    }

    private suspend fun createAndSaveInvoice(request: CreateInvoiceRequest): Invoice {
        val invoice = Invoice.createInvoice(request.invoiceDate)
        invoiceGateway.save(invoice)
        return invoice
    }

    private suspend fun createAndSaveInvoiceLines(
        lines: List<CreateInvoiceRequestLine>,
        invoiceId: InvoiceId
    ) {
        val invoiceLines = createInvoiceLines(lines, invoiceId)

        invoiceLineGateway.saveMultiple(invoiceLines)
    }

    private fun createInvoiceLines(
        request: List<CreateInvoiceRequestLine>,
        invoiceId: InvoiceId
    ): List<InvoiceLine> {
        return request.mapIndexed { index, createInvoiceRequestLine ->
            InvoiceLine.create(
                invoiceId,
                index + 1,
                createInvoiceRequestLine.description,
                createInvoiceRequestLine.price
            )
        }
    }
}

data class CreateInvoiceRequest(
    val invoiceDate: LocalDate,
    val lines: List<CreateInvoiceRequestLine>
)

data class CreateInvoiceRequestLine(
    val description: String,
    val price: BigDecimal
)

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
