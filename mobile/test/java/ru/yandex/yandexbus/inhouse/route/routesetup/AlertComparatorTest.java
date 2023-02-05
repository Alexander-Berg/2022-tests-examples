package ru.yandex.yandexbus.inhouse.route.routesetup;


import com.yandex.mapkit.Time;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.yandexbus.inhouse.model.alert.Closed;
import ru.yandex.yandexbus.inhouse.model.alert.ClosedUntil;
import ru.yandex.yandexbus.inhouse.model.alert.LastTrip;
import ru.yandex.yandexbus.inhouse.model.alert.UnclassifiedAlert;
import ru.yandex.yandexbus.inhouse.route.variants.AlertComparator;

public class AlertComparatorTest {

    private AlertComparator comparator;

    @Before
    public void setUp() throws Exception {
        comparator = AlertComparator.INSTANCE;
    }

    @Test
    public void compareSameAlerts() throws Exception {
        Closed left = new Closed("Sorry, it's closed", new Time());
        Closed right = new Closed("Sorry, it's closed", new Time());

        final int expected = 0;
        Assert.assertEquals(expected, comparator.compare(left, right));
    }

    @Test
    public void closedHasTheHighestPriority() throws Exception {
        Closed closed = new Closed("Sorry, it's closed", new Time());

        // Compare with closed until
        ClosedUntil closedUntil = new ClosedUntil("Closed until", new Time());
        Assert.assertTrue(comparator.compare(closed, closedUntil) > 0);

        // Compare with last trip
        LastTrip lastTrip = new LastTrip("LastTrip", new Time());
        Assert.assertTrue(comparator.compare(closed, lastTrip) > 0);

        // Compare with unknown alert
        UnclassifiedAlert unclassified = new UnclassifiedAlert("Unknown alert");
        Assert.assertTrue(comparator.compare(closed, unclassified) > 0);
    }

    @Test
    public void unclassifiedAlertHasTheLeastPriority() throws Exception {
        UnclassifiedAlert unclassified = new UnclassifiedAlert("Unknown alert");

        Closed closed = new Closed("Sorry, it's closed", new Time());
        Assert.assertTrue(comparator.compare(unclassified, closed) < 0);

        // Compare with closed until
        ClosedUntil closedUntil = new ClosedUntil("Closed until", new Time());
        Assert.assertTrue(comparator.compare(unclassified, closedUntil) < 0);

        // Compare with last trip
        LastTrip lastTrip = new LastTrip("LastTrip", new Time());
        Assert.assertTrue(comparator.compare(unclassified, lastTrip) < 0);
    }


}
