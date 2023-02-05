package ru.yandex.market.processor.testinstance.adapters

import javax.annotation.processing.Messager
import javax.inject.Inject
import javax.tools.Diagnostic

internal class InstanceAdaptersInitializer @Inject constructor(
    private val registry: InternalInstanceAdaptersRegistry,
    private val availableAdapters: Iterable<AdapterRecord>,
    private val logger: Messager
) {

    fun setupProviders() {
        for ((provider, shortcuts) in availableAdapters) {
            try {
                registerAdapters(provider, shortcuts)
            } catch (e: Exception) {
                logger.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Произошла ошибка во время инициализации адаптеров: $e."
                )
            }
        }
    }

    private fun registerAdapters(provider: InstanceAdapter, shortcuts: Iterable<AdapterShortcut>) {
        registry.registerProvider(provider)
        for (shortcut in shortcuts) {
            when (shortcut) {
                is TypeKindShortcut ->
                    registry.registerTypeKindShortcut(provider, shortcut.typeKind)

                is DeclaredTypeShortcut ->
                    registry.registerDeclaredTypeShortcut(provider, shortcut.canonicalName)
            }
        }
    }
}