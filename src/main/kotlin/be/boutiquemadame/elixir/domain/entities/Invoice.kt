package be.boutiquemadame.elixir.domain.entities

import java.time.LocalDate
import java.util.*

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
    val invoiceDate: LocalDate
) {

    companion object Factory {
        fun createInvoice(invoiceDate: LocalDate): Invoice {
            return Invoice(InvoiceId.generate(), invoiceDate)
        }
    }
}

