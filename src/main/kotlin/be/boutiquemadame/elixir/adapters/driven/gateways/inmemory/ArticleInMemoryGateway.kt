package be.boutiquemadame.elixir.adapters.driven.gateways.inmemory

import be.boutiquemadame.elixir.domain.contracts.ArticleGateway
import be.boutiquemadame.elixir.domain.entities.Article
import be.boutiquemadame.elixir.domain.entities.ArticleId

class ArticleInMemoryGateway : ArticleGateway {
    private val articles: HashMap<ArticleId, Article> = hashMapOf()

    override suspend fun getOne(id: ArticleId): Article? {
        return articles[id]
    }

    override suspend fun save(article: Article) {
        articles[article.getId()] = article
    }
}
