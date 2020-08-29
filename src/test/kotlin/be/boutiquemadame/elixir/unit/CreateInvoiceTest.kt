package be.boutiquemadame.elixir.unit

import arrow.core.Either
import arrow.core.orNull
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.ArticleInMemoryGateway
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceInMemoryGateway
import be.boutiquemadame.elixir.adapters.driven.gateways.inmemory.InvoiceLineInMemoryGateway
import be.boutiquemadame.elixir.domain.contracts.ArticleGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceGateway
import be.boutiquemadame.elixir.domain.contracts.InvoiceLineGateway
import be.boutiquemadame.elixir.domain.entities.Invoice
import be.boutiquemadame.elixir.domain.entities.InvoiceId
import be.boutiquemadame.elixir.domain.entities.InvoiceLine
import be.boutiquemadame.elixir.domain.entities.InvoiceProductLine
import be.boutiquemadame.elixir.domain.entities.InvoiceTextLine
import be.boutiquemadame.elixir.domain.entities.Product
import be.boutiquemadame.elixir.usecases.*
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal
import java.time.LocalDate

class CreateInvoiceTest : BehaviorSpec({
    val articleGateway: ArticleGateway = ArticleInMemoryGateway()
    val invoiceGateway: InvoiceGateway = InvoiceInMemoryGateway()
    val invoiceLineGateway: InvoiceLineGateway = InvoiceLineInMemoryGateway()

    val createInvoiceUseCase = CreateInvoiceUseCase(articleGateway, invoiceGateway, invoiceLineGateway)

    Given("I want to create an invoice") {

        val invoiceDate = LocalDate.of(2010, 1, 1)
        val invoiceVatAmount = BigDecimal("21.57")
        val invoiceLines: MutableList<CreateInvoiceRequestLine> = mutableListOf()

        suspend fun createInvoice(): Either<CreateInvoiceError, CreateInvoiceResponse> {
            val createInvoiceRequest = CreateInvoiceRequest(invoiceDate, invoiceVatAmount, invoiceLines)
            return createInvoiceUseCase.execute(createInvoiceRequest)
        }

        suspend fun getSavedInvoice(response: CreateInvoiceResponse?): Invoice? {
            if(response == null) return null
            val invoiceId = InvoiceId.fromString(response.id)
            return invoiceGateway.getOne(invoiceId)
        }

        suspend fun getSavedLines(response: CreateInvoiceResponse?): List<InvoiceLine> {
            if(response == null) return listOf();
            val invoiceId = InvoiceId.fromString(response.id)
            return invoiceLineGateway.getByInvoiceId(invoiceId)
        }

        suspend fun getSavedArticle(line: InvoiceProductLine): Product? {
            return articleGateway.getOne(line.productId) as? Product
        }

        And("The invoice has no lines") {

            When("I save it") {
                val response = createInvoice()
                val savedInvoice = getSavedInvoice(response.orNull())

                Then("An invoice should have been saved") {
                    savedInvoice shouldNotBe null
                }

                Then("The invoice date should be saved") {
                    savedInvoice?.invoiceDate shouldBe invoiceDate
                }

                Then("The VAT amount should be saved") {
                    savedInvoice?.vatAmount shouldBe invoiceVatAmount
                }

                Then("The excluding VAT amount should 0") {
                    savedInvoice?.amountExcludingVat shouldBe BigDecimal(0)
                }

                Then("The total amount should be equals to the VAT amount") {
                    savedInvoice?.totalAmount shouldBe invoiceVatAmount
                }
            }
        }

        And("The invoice has a single text line") {
            val lineDescription = "Lorem ipsum"
            var linePrice = BigDecimal("12.5")

            fun addLine() {
                invoiceLines += CreateInvoiceRequestTextLine(lineDescription, linePrice)
            }


            When("The invoice is saved") {
                addLine()
                val response = createInvoice()
                val savedInvoice = getSavedInvoice(response.orNull())
                val savedLines = getSavedLines(response.orNull())
                val savedFirstLine = savedLines[0] as? InvoiceTextLine

                Then("The invoice should have one line") {
                    savedLines shouldHaveSize 1
                }

                Then("The saved line should be a text line") {
                    savedFirstLine.shouldBeInstanceOf<InvoiceTextLine>()
                }

                Then("The line description should be saved") {
                    savedFirstLine?.description shouldBe lineDescription
                }

                Then("The line price should be saved") {
                    savedFirstLine?.price shouldBe linePrice
                }

                Then("The invoice excluding vat amount should be the line price") {
                    savedInvoice?.amountExcludingVat shouldBe linePrice
                }

                Then("The invoice total amount should be the line price + the VAT") {
                    savedInvoice?.totalAmount shouldBe linePrice + invoiceVatAmount
                }
            }

            And("The price is negative") {
                linePrice = BigDecimal(-12)
                val response = createInvoice()

                When("The invoice is saved") {
                    addLine()

                    Then("An negative price error should be thrown") {
                        response shouldBeLeft {}
                    }
                }
            }
        }

        And("The invoice has a single line with a new article") {
            val lineModel = "Belt"
            val lineArticleNumber = "15E8Z8"
            val lineColor = "RED"
            val lineSize = "36"
            val lineQuantity = 2
            val lineUnitPrice = BigDecimal("12.5")
            invoiceLines += CreateInvoiceRequestLineWithNewProduct(
                model = lineModel,
                articleNumber = lineArticleNumber,
                size = lineSize,
                color = lineColor,
                quantity = lineQuantity,
                unitPrice = lineUnitPrice
            )

            When("The invoice is saved") {
                val response = createInvoice()
                val savedInvoice = getSavedInvoice(response.orNull())
                val savedLines = getSavedLines(response.orNull())
                val savedFirstLine = savedLines[0] as? InvoiceProductLine
                val savedProduct = if (savedFirstLine == null) null else getSavedArticle(savedFirstLine)

                Then("The invoice should have one line") {
                    savedLines shouldHaveSize 1
                }

                Then("The saved line should be a product line") {
                    savedFirstLine.shouldBeInstanceOf<InvoiceProductLine>()
                }

                Then("The line quantity should be saved") {
                    savedFirstLine?.quantity shouldBe 2
                }

                Then("The line unit price should be saved") {
                    savedFirstLine?.unitPrice shouldBe lineUnitPrice
                }

                Then("A new product should have been saved") {
                    savedProduct shouldNotBe null
                }

                Then("The product model should be saved") {
                    savedProduct?.model shouldBe lineModel
                }

                Then("The product article number should be saved") {
                    savedProduct?.articleNumber shouldBe lineArticleNumber
                }

                Then("The product color should be saved") {
                    savedProduct?.color shouldBe lineColor
                }

                Then("The product size should be saved") {
                    savedProduct?.size shouldBe lineSize
                }

                Then("The invoice excluding-vat amount should be correct") {
                    savedInvoice?.amountExcludingVat shouldBe lineUnitPrice * BigDecimal(lineQuantity)
                }
            }
        }

        And("The invoice has a single line with an existing article") {

        }

        And("The invoice has multiple lines") {
            for(i in 0..4) {
                invoiceLines += CreateInvoiceRequestTextLine(
                    description = "Item $i",
                    price = BigDecimal(i + 1)
                )
            }

            When("The invoice is created") {
                val response = createInvoice()
                val savedInvoice = getSavedInvoice(response.orNull())
                val savedLines = getSavedLines(response.orNull())

                Then("Lines should be numbered incrementally starting at 1") {
                    val lineNumbers = savedLines.map { line -> line.getId().lineNumber }

                    lineNumbers shouldBe (1..5)
                }

                Then("Lines ordering should be kept") {
                    val lineDescriptions = savedLines.map { line -> (line as? InvoiceTextLine)?.description }

                    lineDescriptions shouldBe (0..4).map { i -> "Item $i"}
                }

                Then("Amount excluding vat should be correct") {
                    savedInvoice?.amountExcludingVat shouldBe BigDecimal(15)
                }
            }
        }
    }

/*

    @Nested
    inner class WithLinesWithNewProduct {
        @Test
        fun shouldNotAcceptNegativePrice() {
            shouldThrow<NegativePriceError> {
                runBlocking {
                    val unitPrice = BigDecimal(-12)

                    val line = buildLineWithNewProduct(unitPrice = unitPrice)

                    createInvoiceWithOneLine(line)
                }
            }
        }

        /*
        @Test
        fun shouldRejectIfTheSameProductAlreadyExists() {
            shouldThrow<AlreadyExistingProduct> {
                runBlocking {

                }
            }
        }
       */
    }

*/
})
