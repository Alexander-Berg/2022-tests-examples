package ru.yandex.yandexbus.inhouse.route.routesetup.masstransit;

import androidx.annotation.NonNull;
import com.google.common.collect.Lists;
import com.yandex.mapkit.Time;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo;
import ru.yandex.yandexbus.inhouse.model.VehicleType;
import ru.yandex.yandexbus.inhouse.model.alert.Closed;
import ru.yandex.yandexbus.inhouse.model.alert.ThreadAlert;
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel;
import ru.yandex.yandexbus.inhouse.model.route.RouteModel;
import ru.yandex.yandexbus.inhouse.model.route.RouteModelExtensionsKt;
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation;
import ru.yandex.yandexbus.inhouse.route.preview.RouteCitiesInfo;
import ru.yandex.yandexbus.inhouse.route.routesetup.item.routes.MasstransitRouteItem;
import rx.subjects.BehaviorSubject;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecommendedAlertTest {

    private final RouteCitiesInfo routeCitiesInfo = point -> CityLocationInfo.Companion.getUNKNOWN();

    @Test
    public void shouldShowFirstRecommendedAlert() {
        final RouteModel.RouteSection nonRecommendedSection = mockNonRecommendedSection();

        final ThreadAlert expectedAlert = new Closed("Expected alert text", new Time());
        final RouteModel.RouteSection recommendedSection = mockRecommendedSection(expectedAlert);

        MasstransitRouteModel route = mock(MasstransitRouteModel.class);
        when(route.getRouteSections()).thenReturn(Arrays.asList(nonRecommendedSection, recommendedSection));

        final MasstransitRouteItem item = new MasstransitRouteItem(
                route,
                TimeLimitation.Companion.departureNow(),
                routeCitiesInfo,
                BehaviorSubject.create(),
                false);

        final ThreadAlert actual = RouteModelExtensionsKt.getFirstRecommendedTransportAlert(item.getRoute());
        Assert.assertNotNull(actual);
        Assert.assertEquals(expectedAlert, actual);
    }

    @NonNull
    private RouteModel.RouteSection mockNonRecommendedSection() {
        final RouteModel.RouteSection nonRecommendedSection = mock(RouteModel.RouteSection.class);
        when(nonRecommendedSection.getRecommendedTransport()).thenReturn(null);
        when(nonRecommendedSection.getRecommendedTransportsAlerts()).thenReturn(Collections.emptyList());
        return nonRecommendedSection;
    }

    @NonNull
    private RouteModel.RouteSection mockRecommendedSection(ThreadAlert expectedAlert) {
        RouteModel.Transport recommendedTransport = mock(RouteModel.Transport.class);
        when(recommendedTransport.getType()).thenReturn(VehicleType.BUS);
        when(recommendedTransport.isRecommended()).thenReturn(true);
        when(recommendedTransport.getAlerts()).thenReturn(Lists.newArrayList(expectedAlert));

        RouteModel.RouteSection recommendedSection = mock(RouteModel.RouteSection.class);
        when(recommendedSection.getRecommendedTransport()).thenReturn(recommendedTransport);
        when(recommendedSection.getRecommendedTransportsAlerts()).thenReturn(Lists.newArrayList(expectedAlert));
        return recommendedSection;
    }
}
