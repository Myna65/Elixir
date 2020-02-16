package be.boutiquemadame.elixir.adapters.driven.gateways.inmemory

import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.entities.Invoice
import be.boutiquemadame.elixir.domain.entities.InvoiceId

class InvoiceInMemoryGateway : InvoiceGateway {
    private val invoices = HashMap<InvoiceId, Invoice>()

    override suspend fun getOne(id: InvoiceId): Invoice? {
        return invoices[id]
    }

    override suspend fun save(invoice: Invoice) {
        invoices[invoice.id] = invoice
    }
}
