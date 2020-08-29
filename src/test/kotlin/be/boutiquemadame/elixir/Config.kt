package be.boutiquemadame.elixir

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

object Config : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val isolationMode = IsolationMode.InstancePerLeaf
}
