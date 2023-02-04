package com.yandex.mobile.job.address;

import android.graphics.Rect;

import com.yandex.mobile.job.model.Address;
import com.yandex.mobile.job.utils.MapHelper;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

/**
 * TODO: Description is here
 *
 * @author Veretennikov Stanislav <ironbcc@yandex-team.ru> on 14.04.2015
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class MapTest {
    @Test
    public void rectTest() {
        Address.List addresses = new Address.List();
        fillDemoAddresses(addresses);
        Rect region = MapHelper.getRegion(addresses.getGeoPoints());
        Assert.assertEquals(new Rect(1256105405, 628901458, 1253976946, 624553013), region);
    }

    @Test
    public void zoomLevelTest() {
        Rect screenSize = new Rect(0, 0, 720, 1230);
        int density = 2;
        Address.List addresses = new Address.List();
        fillDemoAddresses(addresses);
        Rect region = MapHelper.getRegion(addresses.getGeoPoints());
        Assert.assertEquals(0, MapHelper.getZoomForRegion(screenSize, density, region));

        addresses.clear();
        Address address = new Address();
        address.lat = 55.77108383178711f;
        address.lng = 37.73185729980469f;
        addresses.add(address);
        region = MapHelper.getRegion(addresses.getGeoPoints());
        Assert.assertEquals(17, MapHelper.getZoomForRegion(screenSize, density, region));
    }

    private ArrayList<Address> fillDemoAddresses(ArrayList<Address> addresses) {
        Address address = new Address();
        address.address = "улица Кораблестроителей";
        address.lat = 59.946847f;
        address.lng = 30.214277f;
        addresses.add(0, address);
        address = new Address();
        address.address = "Осиновая улица";
        address.lat = 60.088905f;
        address.lng = 30.353223f;
        addresses.add(0, address);
        address = new Address();
        address.address = "Вишневая улица";
        address.lat = 59.962180f;
        address.lng = 30.571084f;
        addresses.add(address);
        address = new Address();
        address.address = "Пушкинская улица";
        address.lat = 59.722771f;
        address.lng = 30.403670f;
        addresses.add(address);
        address = new Address();
        address.address = "Москва";
        address.lat = 60.088905334472656f;
        address.lng = 30.35322380065918f;
        addresses.add(address);
        return addresses;
    }
}
