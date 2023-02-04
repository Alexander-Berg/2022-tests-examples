package com.yandex.mobile.job.address;

import com.yandex.mobile.job.model.Address;
import com.yandex.mobile.job.utils.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO: Description is here
 *
 * @author Veretennikov Stanislav <ironbcc@yandex-team.ru> on 10.04.2015
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class AddressSortTest {
    @Test
    public void boundaryTest() {
        final Address.List addresses = new Address.List();
        assertTrue(Utils.isEmpty(addresses));
        addresses.sort(null, null);
        assertTrue(Utils.isEmpty(addresses));
    }

    @Test
    public void smallTest() {
        final Address.List addresses = new Address.List();
        addresses.add(create(null, null, true, false));
        addresses.add(create(1, null, true, false));
        addresses.add(create(null, 2l, true, false));
        addresses.add(create(1, 2l, false, false));
        addresses.sort(null, null);

        test(addresses.get(0), 1, null, true, false);
        test(addresses.get(3), 1, 2l, false, false);
    }

    @Test
    public void commutativityTest() {
        final Address.List addresses = new Address.List();
        addresses.add(create(null, null, true, false));
        addresses.add(create(1, null, true, false));
        addresses.sort(null, null);

        test(addresses.get(0), 1, null, true, false);
        test(addresses.get(1), null, null, true, false);

        addresses.clear();
        addresses.add(create(1, null, true, false));
        addresses.add(create(null, null, true, false));
        addresses.sort(null, null);

        test(addresses.get(0), 1, null, true, false);
        test(addresses.get(1), null, null, true, false);
    }

    @Test
    public void completeTest() {
        final Address.List addresses = new Address.List();
        addresses.add(create(null, null, true, true));
        addresses.add(create(1, null, true, false));
        addresses.sort(null, null);

        test(addresses.get(0), null, null, true, true);
        test(addresses.get(1), 1, null, true, false);

        addresses.add(create(1, null, true, true));
        addresses.sort(null, null);

        test(addresses.get(0), 1, null, true, true);
        test(addresses.get(1), null, null, true, true);
        test(addresses.get(2), 1, null, true, false);


        addresses.add(create(2, 3l, true, true));
        addresses.sort(null, null);

        test(addresses.get(0), 2, 3l, true, true);
        test(addresses.get(1), 1, null, true, true);
        test(addresses.get(2), null, null, true, true);
        test(addresses.get(3), 1, null, true, false);

        addresses.sort(1, null);
        test(addresses.get(0), 1, null, true, true);
        test(addresses.get(1), 2, 3l, true, true);
        test(addresses.get(2), null, null, true, true);
        test(addresses.get(3), 1, null, true, false);

        addresses.sort(1, 3l);
        test(addresses.get(0), 1, null, true, true);
        test(addresses.get(1), 2, 3l, true, true);
        test(addresses.get(2), null, null, true, true);
        test(addresses.get(3), 1, null, true, false);

        addresses.sort(2, 3l);
        test(addresses.get(0), 2, 3l, true, true);
        test(addresses.get(1), 1, null, true, true);
        test(addresses.get(2), null, null, true, true);
        test(addresses.get(3), 1, null, true, false);

        addresses.sort(null, 3l);
        test(addresses.get(0), 2, 3l, true, true);
        test(addresses.get(1), 1, null, true, true);
        test(addresses.get(2), null, null, true, true);
        test(addresses.get(3), 1, null, true, false);
    }

    private Address create(Integer metro, Long district, boolean name, boolean coordinate) {
        Address address = new Address();
        if(name) {
            address.address = "ADDRESS";
        }
        if(metro != null) {
            address.metro = new ArrayList(1);
            address.metro.add(metro);
        }
        if(district != null) {
            address.districtId = district;
        }
        if(coordinate) {
            address.lat = 1f;
            address.lng = 1f;
        }
        return address;
    }

    private void test(Address address, Integer metro, Long district, boolean name, boolean coordinate) {
        assertNotNull(address);
        if(metro != null) {
            assertTrue(!Utils.isEmpty(address.metro));
            assertEquals(metro, address.metro.get(0));
        }
        if(district != null) {
            assertEquals(district.longValue(), address.districtId);
        }
        if(name) {
            assertTrue(!Utils.isEmpty(address.address));
        }
        if(coordinate) {
            assertTrue(address.isSuitable());
        }
    }
}
