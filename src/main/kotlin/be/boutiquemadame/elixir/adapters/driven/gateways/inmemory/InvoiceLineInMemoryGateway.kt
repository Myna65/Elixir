package be.boutiquemadame.elixir.adapters.driven.gateways.inmemory

import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.InvoiceId
import be.boutiquemadame.elixir.domain.entities.InvoiceLine
import be.boutiquemadame.elixir.domain.entities.InvoiceLineId

class InvoiceLineInMemoryGateway() : InvoiceLineGateway {
    private val invoiceLines = HashMap<InvoiceLineId, InvoiceLine>()

    override suspend fun getByInvoiceId(invoiceId: InvoiceId): List<InvoiceLine> {
        return invoiceLines.values.toList().filter {
            invoiceLine -> invoiceLine.getId().invoiceId == invoiceId
        }.sortedBy { it.getId().lineNumber }
    }

    override suspend fun saveMultiple(invoiceLinesToAdd: List<InvoiceLine>) {
        invoiceLinesToAdd.forEach {
            invoiceLine -> invoiceLines[invoiceLine.getId()] = invoiceLine
        }
    }
}
