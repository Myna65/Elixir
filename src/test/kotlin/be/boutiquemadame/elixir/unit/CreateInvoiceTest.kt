package be.boutiquemadame.elixir.unit

import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceInMemoryGateway
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceLineInMemoryGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.Invoice
import be.boutiquemadame.elixir.domain.entities.InvoiceId
import be.boutiquemadame.elixir.domain.entities.InvoiceLine
import be.boutiquemadame.elixir.usecases.CreateInvoiceRequest
import be.boutiquemadame.elixir.usecases.CreateInvoiceRequestLine
import be.boutiquemadame.elixir.usecases.CreateInvoiceResponse
import be.boutiquemadame.elixir.usecases.CreateInvoiceUseCase
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreateInvoiceTest {
    private var invoiceGateway: InvoiceGateway = InvoiceInMemoryGateway()
    private var invoiceLineGateway: InvoiceLineGateway = InvoiceLineInMemoryGateway()

    @BeforeEach
    fun setupAdapters() {
        invoiceGateway = InvoiceInMemoryGateway()
        invoiceLineGateway = InvoiceLineInMemoryGateway()
    }

    suspend fun createInvoice(
        invoiceDate: LocalDate = LocalDate.of(2010, 1, 1),
        lines: List<CreateInvoiceRequestLine> = listOf()
    ): CreateInvoiceResponse {
        val createInvoiceUseCase = CreateInvoiceUseCase(invoiceGateway, invoiceLineGateway)
        val createInvoiceRequest = CreateInvoiceRequest(invoiceDate, lines)
        return createInvoiceUseCase.execute(createInvoiceRequest)
    }

    @Nested
    inner class WithoutLines {

        @Test
        fun shouldBeCreated() {
            runBlocking {
                val invoiceResponse = createInvoice()

                invoiceShouldExists(invoiceResponse.id)
            }
        }

        @Test
        fun shouldSaveInvoiceDate() {
            runBlocking {
                val date = LocalDate.of(2019, 2, 20)

                val invoiceResponse = createInvoice(date)

                invoiceShouldHaveInvoiceDate(invoiceResponse.id, date)
            }
        }

        private suspend fun invoiceShouldExists(invoiceId: String) {
            val recoveredInvoice = getInvoiceFromInvoiceStringId(invoiceId)

            recoveredInvoice shouldNotBe null
        }

        private suspend fun invoiceShouldHaveInvoiceDate(invoiceId: String, invoiceDate: LocalDate) {
            val recoveredInvoice = getInvoiceFromInvoiceStringId(invoiceId)

            recoveredInvoice?.invoiceDate shouldBe invoiceDate
        }
    }

    @Nested
    inner class WithoutLinesWithInventoryModification {

        @Test
        fun shouldSaveLines() {
            runBlocking {
                val description = "Lorem ipsum"
                val line = CreateInvoiceRequestLine(description)

                val invoiceResponse = createInvoice(lines = listOf(line))

                val invoiceLines = getInvoiceLinesFromInvoiceStringId(invoiceResponse.id)

                invoiceLines.getOrNull(0)?.description shouldBe description
            }
        }

        @Test
        fun shouldIncrementLinesNumbersFromOne() {
            runBlocking {
                val invoiceResponse = createInvoiceWithNLines(5)

                lineNumberShouldIncrement(invoiceResponse)
            }
        }

        @Test
        fun shouldKeepLinesOrdering() {
            runBlocking {
                val invoiceResponse = createInvoiceWithNLines(4)

                lineOrderingShouldBeUnchanged(invoiceResponse)
            }
        }

        private suspend fun lineNumberShouldIncrement(invoiceResponse: CreateInvoiceResponse) {
            val invoiceLines = getInvoiceLinesFromInvoiceStringId(invoiceResponse.id)

            val lineNumbers = invoiceLines.map {
                it.id.lineNumber
            }

            lineNumbers shouldBe (1..5).toList()
        }

        private suspend fun createInvoiceWithNLines(number: Int): CreateInvoiceResponse {
            val lines = makeLinesForRequest(number)
            return createInvoice(lines = lines)
        }

        private suspend fun lineOrderingShouldBeUnchanged(invoiceResponse: CreateInvoiceResponse) {
            val invoiceLines = getInvoiceLinesFromInvoiceStringId(invoiceResponse.id)

            val lineDescriptions = invoiceLines.map {
                it.description
            }

            lineDescriptions.forEachIndexed { index, description ->
                description shouldContain (index + 1).toString()
            }
        }

        private fun makeLinesForRequest(count: Int): List<CreateInvoiceRequestLine> {
            return (1..count).map {
                CreateInvoiceRequestLine("Description $it")
            }
        }
    }

    private suspend fun getInvoiceFromInvoiceStringId(id: String): Invoice? {
        val invoiceId = InvoiceId.fromString(id)

        return invoiceGateway.getOne(invoiceId)
    }

    private suspend fun getInvoiceLinesFromInvoiceStringId(id: String): List<InvoiceLine> {
        val invoiceId = InvoiceId.fromString(id)

        return invoiceLineGateway.getLinesByInvoiceId(invoiceId)
    }


}
