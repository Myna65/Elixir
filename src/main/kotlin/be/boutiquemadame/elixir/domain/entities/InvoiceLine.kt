package be.boutiquemadame.elixir.domain.entities

import java.math.BigDecimal

data class InvoiceLine(
    val id: InvoiceLineId,
    val description: String,
    val price: BigDecimal
) {

    companion object {
        fun create(
            invoiceId: InvoiceId,
            lineNumber: Int,
            description: String,
            price: BigDecimal
            ): InvoiceLine {

            val invoiceLineId = InvoiceLineId.fromInvoiceIdAndLineNumber(invoiceId, lineNumber)

            return InvoiceLine(invoiceLineId, description, price)
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
