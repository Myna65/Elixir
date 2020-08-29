package be.boutiquemadame.elixir.domain.entities

import arrow.core.Either
import be.boutiquemadame.elixir.usecases.CreateInvoiceError
import java.math.BigDecimal

sealed class InvoiceLine {
    abstract fun getId(): InvoiceLineId
}

data class InvoiceTextLine(
    private val id: InvoiceLineId,
    val description: String,
    val price: BigDecimal
) : InvoiceLine() {

    override fun getId(): InvoiceLineId {
        return id
    }

    companion object {
        fun create(
            invoiceId: InvoiceId,
            lineNumber: Int,
            description: String,
            amount: BigDecimal
        ): Either<NegativePriceError, InvoiceLine> {
            if(amount < BigDecimal.ZERO) {
                Either.left(NegativePriceError(lineNumber, amount))
            }

            val invoiceLineId = InvoiceLineId.fromInvoiceIdAndLineNumber(invoiceId, lineNumber)

            return Either.right(InvoiceTextLine(invoiceLineId, description, amount))
        }
    }
}

data class InvoiceProductLine(
    private val id: InvoiceLineId,
    val productId: ArticleId,
    val quantity: Int,
    val unitPrice: BigDecimal
) : InvoiceLine() {

    override fun getId(): InvoiceLineId {
        return id
    }

    companion object {
        fun create(
            invoiceId: InvoiceId,
            lineNumber: Int,
            productId: ArticleId,
            quantity: Int,
            unitPrice: BigDecimal
        ): InvoiceLine {
            val invoiceLineId = InvoiceLineId.fromInvoiceIdAndLineNumber(invoiceId, lineNumber)

            return InvoiceProductLine(invoiceLineId, productId, quantity, unitPrice)
        }
    }
}

data class InvoiceLineId(val invoiceId: InvoiceId, val lineNumber: Int) {
    companion object {
        fun fromInvoiceIdAndLineNumber(invoiceId: InvoiceId, lineNumber: Int): InvoiceLineId {
            return InvoiceLineId(invoiceId, lineNumber)
        }
    }
}

data class NegativePriceError(val lineIndex: Int, val price: BigDecimal)
