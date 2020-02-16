package be.boutiquemadame.elixir.domain.contracts

import be.boutiquemadame.elixir.domain.entities.Article
import be.boutiquemadame.elixir.domain.entities.ArticleId

interface ArticleGateway {
    suspend fun getOne(id: ArticleId): Article?
    suspend fun save(article: Article)
}
