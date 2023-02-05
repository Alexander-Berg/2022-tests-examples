package ru.yandex.market.processor.testinstance

import dagger.Binds
import dagger.Module
import dagger.Provides
import ru.yandex.market.processor.testinstance.adapters.AdapterRecord
import ru.yandex.market.processor.testinstance.adapters.AdapterShortcut
import ru.yandex.market.processor.testinstance.adapters.BigDecimalInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.BooleanInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.DateInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.DeclaredTypeShortcut
import ru.yandex.market.processor.testinstance.adapters.DoubleInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.EnumInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.FloatInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.Function0InstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.GeneratedInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.InstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.InstanceAdaptersRegistry
import ru.yandex.market.processor.testinstance.adapters.IntegerInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.InternalInstanceAdaptersRegistry
import ru.yandex.market.processor.testinstance.adapters.ListInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.LongInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.MapInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.RootInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.SealedClassInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.SetInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.StringInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.ThrowableInstanceAdapter
import ru.yandex.market.processor.testinstance.adapters.TypeKindShortcut
import java.math.BigDecimal
import java.util.Date
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind

@Module
abstract class ProcessorModule {

    @Binds
    internal abstract fun bindsInstanceAdaptersRegistry(
        registry: InternalInstanceAdaptersRegistry?
    ): InstanceAdaptersRegistry?

    @Binds
    internal abstract fun bindsInstanceAdapter(instanceAdapter: RootInstanceAdapter?): InstanceAdapter?

    companion object {
        private const val DEFAULT_METHOD_NAME_SUFFIX = "TestInstance"
        private const val DEFAULT_FILE_NAME_SUFFIX = "TestFactory"
        private const val DEFAULT_HAND_WRITTEN_METHOD_NAME = "testInstance"
        private const val DEFAULT_JVM_METHOD_NAME = "create"

        @Provides
        fun provideProcessorConfiguration(): ProcessorConfiguration {
            return ProcessorConfiguration(
                DEFAULT_METHOD_NAME_SUFFIX,
                DEFAULT_HAND_WRITTEN_METHOD_NAME,
                DEFAULT_FILE_NAME_SUFFIX,
                DEFAULT_FILE_NAME_SUFFIX,
                DEFAULT_JVM_METHOD_NAME
            )
        }

        @Provides
        internal fun provideAvailableAdapterRecords(
            listInstanceAdapter: ListInstanceAdapter,
            enumInstanceAdapter: EnumInstanceAdapter,
            integerInstanceAdapter: IntegerInstanceAdapter,
            longInstanceAdapter: LongInstanceAdapter,
            stringInstanceAdapter: StringInstanceAdapter,
            booleanInstanceAdapter: BooleanInstanceAdapter,
            floatInstanceAdapter: FloatInstanceAdapter,
            doubleInstanceAdapter: DoubleInstanceAdapter,
            bigDecimalInstanceAdapter: BigDecimalInstanceAdapter,
            setInstanceAdapter: SetInstanceAdapter,
            mapInstanceAdapter: MapInstanceAdapter,
            function0InstanceAdapter: Function0InstanceAdapter,
            dateInstanceAdapter: DateInstanceAdapter,
            generatedInstanceAdapter: GeneratedInstanceAdapter,
            sealedClassInstanceAdapter: SealedClassInstanceAdapter,
            throwableInstanceAdapter: ThrowableInstanceAdapter
        ): Iterable<AdapterRecord> {

            return listOf(
                record(generatedInstanceAdapter),
                record(
                    integerInstanceAdapter,
                    DeclaredTypeShortcut(Int::class.java), TypeKindShortcut(TypeKind.INT)
                ),
                record(
                    longInstanceAdapter,
                    DeclaredTypeShortcut(Long::class.java), TypeKindShortcut(TypeKind.LONG)
                ),
                record(
                    booleanInstanceAdapter,
                    DeclaredTypeShortcut(Boolean::class.java),
                    TypeKindShortcut(TypeKind.BOOLEAN)
                ),
                record(
                    floatInstanceAdapter,
                    DeclaredTypeShortcut(Float::class.java),
                    TypeKindShortcut(TypeKind.FLOAT)
                ),
                record(
                    doubleInstanceAdapter,
                    DeclaredTypeShortcut(Double::class.java),
                    TypeKindShortcut(TypeKind.DOUBLE)
                ),
                record(
                    stringInstanceAdapter,
                    DeclaredTypeShortcut(String::class.java),
                    DeclaredTypeShortcut(CharSequence::class.java)
                ),
                record(dateInstanceAdapter, DeclaredTypeShortcut(Date::class.java)),
                record(
                    function0InstanceAdapter,
                    DeclaredTypeShortcut(Function0::class.java)
                ),
                record(
                    bigDecimalInstanceAdapter,
                    DeclaredTypeShortcut(BigDecimal::class.java)
                ),
                record(
                    listInstanceAdapter,
                    DeclaredTypeShortcut(MutableList::class.java),
                    DeclaredTypeShortcut(MutableCollection::class.java)
                ),
                record(
                    setInstanceAdapter,
                    DeclaredTypeShortcut(MutableSet::class.java)
                ),
                record(
                    mapInstanceAdapter,
                    DeclaredTypeShortcut(MutableMap::class.java)
                ),
                record(enumInstanceAdapter),
                record(sealedClassInstanceAdapter),
                record(throwableInstanceAdapter)
            )
        }

        @Provides
        fun provideMessager(processingEnvironment: ProcessingEnvironment): Messager {
            return processingEnvironment.messager
        }

        private fun record(
            adapter: InstanceAdapter,
            shortcut: AdapterShortcut
        ): AdapterRecord {
            return record(adapter, listOf(shortcut))
        }

        private fun record(
            adapter: InstanceAdapter,
            vararg shortcuts: AdapterShortcut
        ): AdapterRecord {
            return record(adapter, listOf(*shortcuts))
        }

        private fun record(
            adapter: InstanceAdapter,
            shortcuts: List<AdapterShortcut> = emptyList()
        ): AdapterRecord {
            return AdapterRecord(adapter, shortcuts)
        }
    }
}