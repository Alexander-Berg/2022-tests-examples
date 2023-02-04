package ru.yandex.realty.beans.developer.office;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

@Getter
@Setter
@Accessors(chain = true)
public class OfficeResponse {

    public static final String OFFICE_TEMPLATE = "mock/developer/officeTemplate.json";
    private static final double MOSCOW_LATITUDE = 55.753960;
    private static final double MOSCOW_LONGITUDE = 37.620393;
    private static final double SPB_LATITUDE = 59.940939;
    private static final double SPB_LONGITUDE = 30.315871;
    private static final double TYIMEN_LATITUDE = 57.137405;
    private static final double TYIMEN_LONGITUDE = 65.563141;
    private static final double KAZAN_LATITUDE = 55.798551;
    private static final double KAZAN_LONGITUDE = 49.106324;

    private String id;
    private String name;
    private String logo;
    private boolean parking;
    private List<String> phones;
    private String regionLink;
    private String address;
    private String userAddress;
    private Coordinates coordinates;
    private WorkTime workTime;
    private Logotype logotype;

    public static OfficeResponse office() {
        return new OfficeResponse();
    }

    public static OfficeResponse moscowOffice() {
        return officeWithCoordinates(MOSCOW_LATITUDE, MOSCOW_LONGITUDE);
    }

    public static OfficeResponse spbOffice() {
        return officeWithCoordinates(SPB_LATITUDE, SPB_LONGITUDE);
    }

    public static OfficeResponse tyumenOffice() {
        return officeWithCoordinates(TYIMEN_LATITUDE, TYIMEN_LONGITUDE);
    }

    public static OfficeResponse kazanOffice() {
        return officeWithCoordinates(KAZAN_LATITUDE, KAZAN_LONGITUDE);
    }

    public static OfficeResponse officeWithCoordinates(double latitude, double longitude) {
        OfficeResponse office = new GsonBuilder().create().fromJson(
                getResourceAsString(OFFICE_TEMPLATE), OfficeResponse.class);
        office.setId(String.valueOf((int) (Math.random() * 10000)));
        office.getCoordinates().setLatitude(latitude + Math.random() * 0.02);
        office.getCoordinates().setLongitude(longitude + Math.random() * 0.02);
        return office;
    }
}
