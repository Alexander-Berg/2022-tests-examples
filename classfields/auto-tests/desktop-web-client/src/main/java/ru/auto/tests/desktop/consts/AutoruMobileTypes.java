package ru.auto.tests.desktop.consts;

import lombok.Getter;
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;

import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
public enum AutoruMobileTypes {

    TRUCK("trucks", "truck", "trucks", CategoryEnum.TRUCKS),
    LCV("trucks", "lcv", "trucks", CategoryEnum.TRUCKS),
    ARTIC("trucks", "artic", "trucks", CategoryEnum.TRUCKS),
    BUS("trucks", "bus", "trucks", CategoryEnum.TRUCKS),
    TRAILER("trucks", "trailer", "trucks", CategoryEnum.TRUCKS),
    MOTORCYCLE("moto", "motorcycle", "motorcycle", CategoryEnum.MOTO),
    SCOOTERS("moto", "scooters", "scooters", CategoryEnum.MOTO),
    SNOWMOBILE("moto", "snowmobile", "snowmobile", CategoryEnum.MOTO),
    ATV("moto", "atv", "atv", CategoryEnum.MOTO),
    CAR("cars", "car", "cars", CategoryEnum.CARS);

    private String type;
    private String subCategory;
    private String urlPath;
    private CategoryEnum category;

    AutoruMobileTypes(String type, String subCategory, String urlPath, CategoryEnum category) {
        this.type = type;
        this.subCategory = subCategory;
        this.urlPath = urlPath;
        this.category = category;
    }

    public static Collection<Object[]> allCategories() {
        return asList(new Object[][]{
                {"trucks", "truck", "trucks", CategoryEnum.TRUCKS},
                {"trucks", "lcv", "trucks", CategoryEnum.TRUCKS},
                {"trucks", "artic", "trucks", CategoryEnum.TRUCKS},
                {"trucks", "bus", "trucks", CategoryEnum.TRUCKS},
                {"trucks", "trailer", "trucks", CategoryEnum.TRUCKS},
                {"moto", "motorcycle", "motorcycle", CategoryEnum.MOTO},
                {"moto", "scooters", "scooters", CategoryEnum.MOTO},
                {"moto", "snowmobile", "snowmobile", CategoryEnum.MOTO},
                {"moto", "atv", "atv", CategoryEnum.MOTO},
                {"cars", "car", "cars", CategoryEnum.CARS}
        });
    }

}
