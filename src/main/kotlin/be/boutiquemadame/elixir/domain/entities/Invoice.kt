package be.boutiquemadame.elixir.domain.entities

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class InvoiceId(val raw: String) {
    companion object Factory {
        fun generate(): InvoiceId {
            val stringId = UUID.randomUUID().toString()
            return InvoiceId(stringId)
        }

        fun fromString(id: String): InvoiceId {
            return InvoiceId(id)
        }
    }
}

data class Invoice(
    val id: InvoiceId,
    val invoiceDate: LocalDate,
    val amountExcludingVat: BigDecimal,
    val vatAmount: BigDecimal,
    val totalAmount: BigDecimal
) {

    companion object Factory {
        fun createInvoice(
            invoiceDate: LocalDate,
            amountExcludingVat: BigDecimal,
            vatAmount: BigDecimal
        ): Invoice {
            val totalAmount = amountExcludingVat + vatAmount

            return Invoice(InvoiceId.generate(), invoiceDate, amountExcludingVat, vatAmount, totalAmount)
        }
    }
}
