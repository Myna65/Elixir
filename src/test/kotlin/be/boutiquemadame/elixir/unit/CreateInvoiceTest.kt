package be.boutiquemadame.elixir.unit

import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.ArticleInMemoryGateway
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceInMemoryGateway
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceLineInMemoryGateway
import be.boutiquemadame.elixir.domain.contracts.ArticleGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.*
import be.boutiquemadame.elixir.usecases.*
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CreateInvoiceTest {
    private var articleGateway: ArticleGateway = ArticleInMemoryGateway()
    private var invoiceGateway: InvoiceGateway = InvoiceInMemoryGateway()
    private var invoiceLineGateway: InvoiceLineGateway = InvoiceLineInMemoryGateway()

    @BeforeEach
    fun setupAdapters() {
        invoiceGateway = InvoiceInMemoryGateway()
        invoiceLineGateway = InvoiceLineInMemoryGateway()
    }

    suspend fun createInvoice(
        invoiceDate: LocalDate = LocalDate.of(2010, 1, 1),
        vatAmount: BigDecimal = BigDecimal(21),
        lines: List<CreateInvoiceRequestLine> = listOf()
    ): CreateInvoiceResponse {
        val createInvoiceUseCase = CreateInvoiceUseCase(articleGateway, invoiceGateway, invoiceLineGateway)
        val createInvoiceRequest = CreateInvoiceRequest(invoiceDate, vatAmount, lines)
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

        @Test
        fun shouldSaveVatAmount() {
            runBlocking {
                val vatAmount = BigDecimal(21.57)

                val invoiceResponse = createInvoice(vatAmount = vatAmount)

                invoiceShouldHaveVatAmount(invoiceResponse.id, vatAmount)
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

        private suspend fun invoiceShouldHaveVatAmount(invoiceId: String, vatAmount: BigDecimal) {
            val recoveredInvoice = getInvoiceFromInvoiceStringId(invoiceId)

            recoveredInvoice?.vatAmount shouldBe vatAmount
        }
    }

    @Nested
    inner class WithLinesWithoutProduct {

        @Test
        fun shouldSaveLinesWithDescription() {
            runBlocking {
                val description = "Lorem ipsum"
                val invoiceResponse = createInvoiceWithOneLineWithoutProduct(description = description)

                firstLineDescriptionShouldBe(invoiceResponse, description)
            }
        }

        @Test
        fun shouldSaveLinePrice() {
            runBlocking {
                val price = BigDecimal(13.5)
                val invoiceResponse = createInvoiceWithOneLineWithoutProduct(price = price)

                firstLinePriceShouldBe(invoiceResponse, price)
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

        @Test
        fun invoiceTotalExcludingVatShouldBeCorrect() {
            runBlocking {
                val line1 = buildLineWithoutProduct(price = BigDecimal(15))
                val line2 = buildLineWithoutProduct(price = BigDecimal(12.34))

                val invoiceResponse = createInvoice(lines = listOf(line1, line2))

                val invoice = getInvoiceFromInvoiceStringId(invoiceResponse.id)

                invoice?.amountExcludingVat shouldBe BigDecimal(15 + 12.34)
            }
        }

        private suspend fun firstLineDescriptionShouldBe(
            invoiceResponse: CreateInvoiceResponse,
            description: String
        ) {
            val firstLine = getInvoiceFirstLineAsWithoutProduct(invoiceResponse)

            firstLine?.description shouldBe description
        }

        private suspend fun firstLinePriceShouldBe(
            invoiceResponse: CreateInvoiceResponse,
            unitPrice: BigDecimal
        ) {
            val firstLine = getInvoiceFirstLineAsWithoutProduct(invoiceResponse)

            firstLine?.price shouldBe unitPrice
        }

        private suspend fun lineNumberShouldIncrement(invoiceResponse: CreateInvoiceResponse) {
            val invoiceLines = getInvoiceLinesWithoutProductFromInvoiceStringId(invoiceResponse.id)

            val lineNumbers = invoiceLines.map {
                it.getId().lineNumber
            }

            lineNumbers shouldBe (1..5).toList()
        }

        private suspend fun createInvoiceWithNLines(number: Int): CreateInvoiceResponse {
            val lines = makeLinesForRequest(number)
            return createInvoice(lines = lines)
        }

        private fun makeLinesForRequest(count: Int): List<CreateInvoiceRequestLineWithoutProduct> {
            return (1..count).map {
                buildLineWithoutProduct(description = "Description $it")
            }
        }

        private suspend fun lineOrderingShouldBeUnchanged(invoiceResponse: CreateInvoiceResponse) {
            val invoiceLines = getInvoiceLinesWithoutProductFromInvoiceStringId(invoiceResponse.id)

            val lineDescriptions = invoiceLines.map {
                it.description
            }

            lineDescriptions.forEachIndexed { index, description ->
                description shouldContain (index + 1).toString()
            }
        }
    }

    @Nested
    inner class WithLinesWithNewProduct {

        @Test
        fun shouldCreateProductWithCorrectArticleNumber() {
            runBlocking {
                val articleNumber = "154E78"

                val line = buildLineWithNewProduct(articleNumber = articleNumber)

                val invoiceResponse = createInvoiceWithOneLine(line)

                productShouldHaveArticleNumber(invoiceResponse.id, articleNumber)
            }
        }

        @Test
        fun shouldCreateProductWithCorrectModel() {
            runBlocking {
                val model = "Skirt"

                val line = buildLineWithNewProduct(model = model)

                val invoiceResponse = createInvoiceWithOneLine(line)

                productShouldHaveModel(invoiceResponse.id, model)
            }
        }

        @Test
        fun shouldCreateProductWithCorrectSize() {
            runBlocking {
                val size = "48"

                val line = buildLineWithNewProduct(size = size)

                val invoiceResponse = createInvoiceWithOneLine(line)

                productShouldHaveSize(invoiceResponse.id, size)
            }
        }

        @Test
        fun shouldCreateProductWithCorrectColor() {
            runBlocking {
                val color = "Red"

                val line = buildLineWithNewProduct(color = color)

                val invoiceResponse = createInvoiceWithOneLine(line)

                productShouldHaveColor(invoiceResponse.id, color)
            }
        }

        @Test
        fun shouldCreateLineWithCorrectPrice() {
            runBlocking {
                val price = BigDecimal(42.42)

                val line = buildLineWithNewProduct(unitPrice = price)

                val invoiceResponse = createInvoiceWithOneLine(line)

                firstLineShouldHaveUnitPrice(invoiceResponse.id, price)
            }
        }

        @Test
        fun shouldCreateLineWithCorrectQuantity() {
            runBlocking {
                val quantity = 13

                val line = buildLineWithNewProduct(quantity = quantity)

                val invoiceResponse = createInvoiceWithOneLine(line)

                firstLineShouldHaveQuantity(invoiceResponse.id, quantity)
            }
        }

        private suspend fun productShouldHaveArticleNumber(
            invoiceId: String,
            articleNumber: String
        ) {
            val product = getProductFromInvoiceFirstLine(invoiceId)

            product.articleNumber shouldBe articleNumber
        }

        private suspend fun productShouldHaveModel(
            invoiceId: String,
            model: String
        ) {
            val product = getProductFromInvoiceFirstLine(invoiceId)

            product.model shouldBe model
        }

        private suspend fun productShouldHaveSize(
            invoiceId: String,
            size: String
        ) {
            val product = getProductFromInvoiceFirstLine(invoiceId)

            product.size shouldBe size
        }

        private suspend fun productShouldHaveColor(
            invoiceId: String,
            color: String
        ) {
            val product = getProductFromInvoiceFirstLine(invoiceId)

            product.color shouldBe color
        }

        private suspend fun firstLineShouldHaveUnitPrice(
            invoiceId: String,
            unitPrice: BigDecimal
        ) {
            val line = getFirstLineAsWithProduct(invoiceId)

            line.unitPrice shouldBe unitPrice
        }

        private suspend fun firstLineShouldHaveQuantity(
            invoiceId: String,
            quantity: Int
        ) {
            val line = getFirstLineAsWithProduct(invoiceId)

            line.quantity shouldBe quantity
        }

        private suspend fun getProductFromInvoiceFirstLine(invoiceId: String): Product {
            val firstLine = getFirstLineAsWithProduct(invoiceId)

            return articleGateway.getOne(firstLine.productId) as Product
        }

        private suspend fun getFirstLineAsWithProduct(invoiceId: String): InvoiceLineWithProduct {
            val lines = getInvoiceLinesFromInvoiceStringId(invoiceId)

            return lines.getOrNull(0) as InvoiceLineWithProduct
        }
    }
    @Nested
    inner class WithLines {

        @Test
        fun shouldComputeTotalAmountCorrectly() {
            runBlocking {
                val line1 = buildLineWithoutProduct("1", BigDecimal(12.5))
                val line2 = buildLineWithNewProduct(articleNumber = "1845", unitPrice = BigDecimal(3), quantity = 10)
                val line3 = buildLineWithoutProduct("3", BigDecimal(-7))

                val lines = listOf(line1, line2, line3)

                val invoiceResponse = createInvoice(lines = lines, vatAmount = BigDecimal(35.45))

                invoiceShouldHaveTotal(invoiceResponse.id, BigDecimal(12.5 + 3 * 10 - 7 + 35.45))
            }
        }

        private suspend fun invoiceShouldHaveTotal(invoiceId: String, total: BigDecimal) {
            val invoice = getInvoiceFromInvoiceStringId(invoiceId)

            invoice?.totalAmount shouldBe total
        }
    }
    private suspend fun getInvoiceFromInvoiceStringId(id: String): Invoice? {
        val invoiceId = InvoiceId.fromString(id)

        return invoiceGateway.getOne(invoiceId)
    }

    private suspend fun getInvoiceLinesFromInvoiceStringId(id: String): List<InvoiceLine> {
        val invoiceId = InvoiceId.fromString(id)

        return invoiceLineGateway.getByInvoiceId(invoiceId)
    }

    private suspend fun createInvoiceWithOneLineWithoutProduct(
        description: String = "Description",
        price: BigDecimal = BigDecimal(10)
    ): CreateInvoiceResponse {
        val line = buildLineWithoutProduct(description, price)
        return createInvoiceWithOneLine(line)
    }

    private suspend fun createInvoiceWithOneLine(
        line: CreateInvoiceRequestLine
    ): CreateInvoiceResponse {
        return createInvoice(lines = listOf(line))
    }

    private fun buildLineWithoutProduct(
        description: String = "Description",
        price: BigDecimal = BigDecimal(10)
    ): CreateInvoiceRequestLineWithoutProduct {
        return CreateInvoiceRequestLineWithoutProduct(description, price)
    }

    private fun buildLineWithNewProduct(
        model: String = "Model",
        articleNumber: String = "123",
        size: String = "Size",
        color: String = "Color",
        quantity: Int = 2,
        unitPrice: BigDecimal = BigDecimal(12.34)

    ): CreateInvoiceRequestLineWithNewProduct {
        return CreateInvoiceRequestLineWithNewProduct(
            model,
            articleNumber,
            size,
            color,
            quantity,
            unitPrice
        )
    }

    private suspend fun getInvoiceFirstLineAsWithoutProduct(invoiceResponse: CreateInvoiceResponse): InvoiceLineWithoutProduct? {
        val invoiceLines = getInvoiceLinesWithoutProductFromInvoiceStringId(invoiceResponse.id)

        return when (val firstLine = invoiceLines.getOrNull(0)) {
            is InvoiceLineWithoutProduct -> firstLine
            else -> null
        }
    }

    private suspend fun getInvoiceLinesWithoutProductFromInvoiceStringId(id: String): List<InvoiceLineWithoutProduct> {
        return getInvoiceLinesFromInvoiceStringId(id).filterIsInstance<InvoiceLineWithoutProduct>()
    }
}
