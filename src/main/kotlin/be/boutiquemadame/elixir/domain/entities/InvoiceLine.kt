package be.boutiquemadame.elixir.domain.entities

data class InvoiceLine(val id: InvoiceLineId, val description: String) {
    companion object {
        fun create(invoiceId: InvoiceId, lineNumber: Int, description: String): InvoiceLine {
            val invoiceLineId = InvoiceLineId.fromInvoiceIdAndLineNumber(invoiceId, lineNumber)
            return InvoiceLine(invoiceLineId, description)
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
