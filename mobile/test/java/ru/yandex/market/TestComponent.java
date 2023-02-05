package ru.yandex.market;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import javax.inject.Singleton;

import ru.yandex.market.base.network.common.address.HttpAddressParser;
import ru.yandex.market.clean.data.mapper.discount.DiscountTypeMapper;
import ru.yandex.market.clean.data.mapper.money.MoneyMapper;
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter;
import ru.yandex.market.common.android.ResourcesManager;
import ru.yandex.market.common.dateformatter.DateFormatter;
import ru.yandex.market.common.preferences.CommonPreferences;
import ru.yandex.market.data.money.mapper.CurrencyMapper;
import ru.yandex.market.data.money.mapper.TermMapper;
import ru.yandex.market.data.money.parser.MoneyAmountParser;
import ru.yandex.market.datetime.DateTimeProvider;
import ru.yandex.market.di.ApplicationModule;
import ru.yandex.market.di.TestAndroidModule;
import ru.yandex.market.di.TestDateFormatterModule;
import ru.yandex.market.di.TestDateTimeModule;
import ru.yandex.market.di.TestGsonModule;
import ru.yandex.market.di.TestMoneyModule;
import ru.yandex.market.di.TestPreferencesModule;
import ru.yandex.market.di.TestSchedulersModule;
import ru.yandex.market.di.module.common.NetworkModule;
import ru.yandex.market.di.module.feature.FeatureConfigurationKotlinModule;
import ru.yandex.market.di.module.feature.FeatureConfigurationModule;
import ru.yandex.market.feature.money.formatter.CurrencyFormatter;
import ru.yandex.market.feature.money.formatter.FormatSymbolsProvider;
import ru.yandex.market.internal.PreferencesDataStore;

import androidx.annotation.NonNull;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Component(
        modules = {
                // production modules
                AndroidInjectionModule.class,
                ApplicationModule.class,
                FeatureConfigurationKotlinModule.class,
                FeatureConfigurationModule.class,
                NetworkModule.class,
                // test modules
                TestAndroidModule.class,
                TestDateTimeModule.class,
                TestDateFormatterModule.class,
                TestMoneyModule.class,
                TestGsonModule.class,
                TestPreferencesModule.class,
                TestSchedulersModule.class,
        }
)
@Singleton
public interface TestComponent extends AndroidInjector<TestApplication> {

    @NonNull
    Resources resources();

    @NonNull
    DateTimeProvider dateTimeProvider();

    @NonNull
    CommonPreferences commonPreferences();

    @NonNull
    PreferencesDataStore preferencesDataStore();

    @NonNull
    FormatSymbolsProvider formatSymbolsProvider();

    @NonNull
    AssetManager assetManager();

    @NonNull
    HttpAddressParser httpAddressParser();

    @NonNull
    Context context();

    @NonNull
    ResourcesManager provideResourcesManager();

    @NonNull
    DateFormatter provideDateFormatter();

    @NonNull
    CurrencyFormatter provideCurrencyFormatter();

    @NonNull
    CurrencyMapper provideCurrencyMapper();

    @NonNull
    DiscountTypeMapper provideDiscountTypeMapper();

    @NonNull
    MoneyAmountParser provideMoneyAmountParser();

    @NonNull
    FormatSymbolsProvider provideFormatSymbolsProvider();

    @NonNull
    TermMapper provideTermMapper();

    @NonNull
    MoneyMapper provideMoneyMapper();

    @NonNull
    MoneyFormatter provideMoneyFormatter();
}
