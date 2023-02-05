package ru.yandex.yandexbus.inhouse.service.location.country;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.yandexbus.inhouse.service.location.country.CountryDetector.CountryData;
import ru.yandex.yandexbus.inhouse.service.location.country.CountryDetector.Source;
import ru.yandex.yandexbus.inhouse.service.location.country.GdprModeProvider.GdprMode;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import static org.mockito.Mockito.when;

public class GdprModeProviderTest {

    @Mock
    private CountryDetector countryDetector;
    private PublishSubject<CountryData> countryDataChanges;
    private TestSubscriber<GdprMode> testSubscriber;

    private GdprModeProvider gdprModeProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        countryDataChanges = PublishSubject.create();
        when(countryDetector.countryDataChanges()).thenReturn(countryDataChanges);

        gdprModeProvider = new GdprModeProvider(countryDetector);
        testSubscriber = TestSubscriber.create();
        gdprModeProvider.modes().subscribe(testSubscriber);
    }

    @Test
    public void noReactionForNoData() {
        testSubscriber.assertNoValues();
    }

    @Test
    public void noReactionForSavedCountryData() {
        changeCountry(Country.RUSSIA, Source.SAVED);
        testSubscriber.assertNoValues();
    }

    @Test
    public void gdprModeChangesForDifferentCountries() {
        changeCountry(Country.RUSSIA, Source.LOCATION);
        testSubscriber.assertValuesAndClear(GdprMode.NOT_GDPR);

        changeCountry(Country.UNKNOWN, Source.LOCATION);
        testSubscriber.assertValuesAndClear(GdprMode.GDPR);
    }

    @Test
    public void noChangesForCountryFromSameCategory() {
        changeCountry(Country.RUSSIA, Source.LOCATION);
        testSubscriber.assertValuesAndClear(GdprMode.NOT_GDPR);

        changeCountry(Country.RUSSIA, Source.LOCATION);
        testSubscriber.assertNoValues();

        changeCountry(Country.BELARUS, Source.LOCATION);
        testSubscriber.assertNoValues();
    }

    private void changeCountry(Country country, Source source) {
        countryDataChanges.onNext(new CountryData(country, source, null));
    }
}