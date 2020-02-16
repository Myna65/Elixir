package be.boutiquemadame.elixir.domain.entities

import java.math.BigDecimal

sealed class InvoiceLine {
    abstract fun getId(): InvoiceLineId
}

data class InvoiceLineWithoutProduct(
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
        ): InvoiceLine {

            val invoiceLineId = InvoiceLineId.fromInvoiceIdAndLineNumber(invoiceId, lineNumber)

            return InvoiceLineWithoutProduct(invoiceLineId, description, amount)
        }
    }
}

data class InvoiceLineWithProduct(
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

            return InvoiceLineWithProduct(invoiceLineId, productId, quantity, unitPrice)
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
