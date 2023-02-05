package ru.yandex.yandexmaps.taxi;

import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.maps.BaseTest;
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point;
import ru.yandex.yandexmaps.multiplatform.core.taxi.OpenTaxiSource;
import ru.yandex.yandexmaps.taxi.api.TaxiUriBuilder;

public class TaxiUriBuilderTests extends BaseTest {

    @Test
    public void yandexTaxiUriPlace() {
        taxiAppUriTestImpl(
                Point.Factory.invoke(55.5, 37.7),
                Point.Factory.invoke(56.6, 38.8),
                OpenTaxiSource.ORGANIZATION_CARD,
                "yandextaxi://route?utm_source=yamaps&ref=2446152&start-lat=55.500000&start-lon=37.700000&end-lat=56.600000&end-lon=38.800000&appmetrica_tracking_id=1178268795219780156"
        );
    }

    @Test
    public void yandexTaxiUriRoute() {
        taxiAppUriTestImpl(
                Point.Factory.invoke(55.5, 37.7),
                Point.Factory.invoke(56.6, 38.8),
                OpenTaxiSource.ROUTE_ALL,
                "yandextaxi://route?utm_source=yamaps&ref=2448186&start-lat=55.500000&start-lon=37.700000&end-lat=56.600000&end-lon=38.800000&appmetrica_tracking_id=1178268795219780156"
        );
    }

    @Test
    public void referralUriPlace() {
        referralUriTestImpl(
                Point.Factory.invoke(55.5, 37.7),
                Point.Factory.invoke(56.6, 38.8),
                OpenTaxiSource.ORGANIZATION_CARD,
                "https://3.redirect.appmetrica.yandex.com/route?utm_source=yamaps&ref=2446152&start-lat=55.500000&start-lon=37.700000&end-lat=56.600000&end-lon=38.800000&appmetrica_tracking_id=1178268795219780156"
        );
    }

    @Test
    public void referralUriRoute() {
        referralUriTestImpl(
                Point.Factory.invoke(55.5, 37.7),
                Point.Factory.invoke(56.6, 38.8),
                OpenTaxiSource.ROUTE_ALL,
                "https://3.redirect.appmetrica.yandex.com/route?utm_source=yamaps&ref=2448186&start-lat=55.500000&start-lon=37.700000&end-lat=56.600000&end-lon=38.800000&appmetrica_tracking_id=1178268795219780156"
        );
    }

    private void taxiAppUriTestImpl(Point from, Point to, OpenTaxiSource source, String expected) {
        final String uri = TaxiUriBuilder.buildUri(from, to, source.getRef());
        checkUriEquality(expected, uri);
    }

    private void referralUriTestImpl(Point from, Point to, OpenTaxiSource source, String expected) {
        final String uri = TaxiUriBuilder.buildReferralUri(from, to, source.getRef());
        checkUriEquality(expected, uri);
    }

    private void checkUriEquality(String expected, String actual) {
        final Uri parsedUri = Uri.parse(actual);
        final Uri parsedExpected = Uri.parse(expected);

        Assert.assertEquals(parsedExpected.getScheme(), parsedUri.getScheme());
        Assert.assertEquals(parsedExpected.getHost(), parsedUri.getHost());
        Assert.assertEquals(parsedExpected.getPath(), parsedUri.getPath());
        Assert.assertEquals(parsedExpected.getQueryParameterNames(), parsedUri.getQueryParameterNames());
        for (String parameter : parsedExpected.getQueryParameterNames()) {
            Assert.assertEquals(parsedExpected.getQueryParameter(parameter), parsedUri.getQueryParameter(parameter));
        }
    }
}
