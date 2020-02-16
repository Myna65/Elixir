package be.boutiquemadame.elixir.shared

interface UseCase<I, O> {
    suspend fun execute(request: I): O
}
