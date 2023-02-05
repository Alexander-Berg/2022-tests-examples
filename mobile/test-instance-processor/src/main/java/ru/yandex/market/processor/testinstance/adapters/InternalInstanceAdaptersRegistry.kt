package ru.yandex.market.processor.testinstance.adapters

import ru.yandex.market.processor.commons.canonicalNameOrNull
import javax.inject.Inject
import javax.inject.Singleton
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@Singleton
internal class InternalInstanceAdaptersRegistry @Inject constructor() : InstanceAdaptersRegistry {
    private val declaredTypeShortcuts = mutableMapOf<String, InstanceAdapter>()
    private val typeKindShortcuts = mutableMapOf<TypeKind, InstanceAdapter>()
    private val providers = mutableListOf<InstanceAdapter>()

    fun registerProvider(adapter: InstanceAdapter) {
        val isNotAdded = providers.none { it.javaClass == adapter.javaClass }
        check(isNotAdded) {
            "Не удалось зарегистрировать адаптер $adapter! Адаптер c таким типом уже зарегистрирован. " +
                    "Проверь конфигурацию адаптеров в модуле dagger."
        }
        providers.add(adapter)
    }

    fun registerDeclaredTypeShortcut(adapter: InstanceAdapter, declaredTypeCanonicalName: String) {
        declaredTypeShortcuts[declaredTypeCanonicalName] = adapter
    }

    fun registerTypeKindShortcut(adapter: InstanceAdapter, typeKind: TypeKind) {
        typeKindShortcuts[typeKind] = adapter
    }

    override fun getProvider(type: TypeMirror): InstanceAdapter? {
        val typeKindShortcut = typeKindShortcuts[type.kind]
        if (typeKindShortcut != null) {
            return typeKindShortcut
        }

        if (type.kind == TypeKind.DECLARED) {
            val declaredTypeShortcut = type.canonicalNameOrNull?.let { declaredTypeShortcuts[it] }
            if (declaredTypeShortcut != null) {
                return declaredTypeShortcut
            }
        }

        return providers.find { it.isTypeSupported(type) }
    }
}