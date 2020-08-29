package be.boutiquemadame.elixir.shared

import arrow.core.Either

interface UseCase<I, E, O> {
    suspend fun execute(request: I): Either<E, O>
}
