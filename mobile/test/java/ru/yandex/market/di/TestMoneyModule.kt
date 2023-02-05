package ru.yandex.market.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.money.formatter.CurrencyFormatter
import ru.yandex.market.data.money.parser.MoneyAmountParser
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.data.money.mapper.CurrencyMapper
import ru.yandex.market.clean.data.mapper.discount.DiscountTypeMapper
import ru.yandex.market.feature.money.formatter.FormatSymbolsProvider
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.data.money.mapper.TermMapper

@Module
object TestMoneyModule {

    @Provides
    @Reusable
    fun provideCurrencyFormatter(resourceManager: ResourcesManager): CurrencyFormatter {
        return CurrencyFormatter(resourceManager)
    }

    @Provides
    @Reusable
    fun provideCurrencyMapper(): CurrencyMapper {
        return CurrencyMapper()
    }

    @Provides
    @Reusable
    fun provideDiscountTypeMapper(): DiscountTypeMapper {
        return DiscountTypeMapper()
    }

    @Provides
    @Reusable
    fun provideMoneyAmountParser(): MoneyAmountParser {
        return MoneyAmountParser()
    }

    @Provides
    @Reusable
    fun provideFormatSymbolsProvider(): FormatSymbolsProvider {
        return FormatSymbolsProvider()
    }

    @Provides
    @Reusable
    fun provideTermMapper(): TermMapper {
        return TermMapper()
    }

    @Provides
    @Reusable
    fun provideMoneyMapper(moneyAmountParser: MoneyAmountParser, currencyMapper: CurrencyMapper): MoneyMapper {
        return MoneyMapper(
            moneyAmountParser,
            currencyMapper
        )
    }

    @Provides
    @Reusable
    fun provideMoneyFormatter(
        resourcesManager: ResourcesManager,
        moneyAmountParser: MoneyAmountParser,
        symbolsProvider: FormatSymbolsProvider,
        currencyFormatter: CurrencyFormatter,
        moneyMapper: MoneyMapper
    ): MoneyFormatter {
        return MoneyFormatter(
            resourcesManager, symbolsProvider, currencyFormatter, moneyMapper
        )
    }
}
