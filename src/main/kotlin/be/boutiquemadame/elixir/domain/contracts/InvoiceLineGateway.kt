package be.boutiquemadame.elixir.domain.contracts

import be.boutiquemadame.elixir.domain.entities.InvoiceId
import be.boutiquemadame.elixir.domain.entities.InvoiceLine

interface InvoiceLineGateway {
    suspend fun getLinesByInvoiceId(invoiceId: InvoiceId): List<InvoiceLine>
    suspend fun saveMultiple(invoiceLinesToAdd: List<InvoiceLine>)
}
