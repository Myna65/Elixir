package be.boutiquemadame.elixir.domain.entities

import java.util.*

data class ArticleId(val raw: String) {
    companion object Factory {
        fun generate(): ArticleId {
            val stringId = UUID.randomUUID().toString()
            return ArticleId(stringId)
        }
    }
}

sealed class Article {
    abstract fun getId(): ArticleId
}

data class Product(
    private val id: ArticleId,
    val model: String,
    val articleNumber: String,
    val size: String,
    val color: String
) : Article() {

    override fun getId(): ArticleId {
        return id
    }

    companion object {
        fun create(
            model: String,
            articleNumber: String,
            size: String,
            color: String
        ): Product {
            return Product(
                ArticleId.generate(),
                model,
                articleNumber,
                size,
                color
            )
        }
    }
}
