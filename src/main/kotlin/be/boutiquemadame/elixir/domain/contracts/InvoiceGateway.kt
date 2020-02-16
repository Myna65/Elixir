package be.boutiquemadame.elixir.domain.contracts

import be.boutiquemadame.elixir.domain.entities.Invoice
import be.boutiquemadame.elixir.domain.entities.InvoiceId

interface InvoiceGateway {
    suspend fun getOne(id: InvoiceId): Invoice?
    suspend fun save(invoice: Invoice)
}
