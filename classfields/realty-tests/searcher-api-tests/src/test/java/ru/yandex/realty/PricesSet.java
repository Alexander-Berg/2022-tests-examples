package ru.yandex.realty;

import org.apache.commons.io.IOUtils;
import ru.yandex.realty.searcher.EstimateCostTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static ru.auto.test.api.realty.RentOfferPlacementPeriod.PER_DAY;
import static ru.auto.test.api.realty.RentOfferPlacementPeriod.PER_MONTH;
import static ru.auto.test.api.realty.RentOfferPlacementPeriod.__EMPTY__;
import static ru.auto.test.api.realty.VosOfferCategory.APARTMENT;
import static ru.auto.test.api.realty.VosOfferCategory.GARAGE;
import static ru.auto.test.api.realty.VosOfferCategory.HOUSE;
import static ru.auto.test.api.realty.VosOfferCategory.LOT;
import static ru.auto.test.api.realty.VosOfferCategory.ROOMS;
import static ru.auto.test.api.realty.VosOfferType.RENT;
import static ru.auto.test.api.realty.VosOfferType.SELL;

/**
 * @author kurau (Yuri Kalinin)
 */
public class PricesSet {

    public static final String FIZLICO = "data/fizlico.txt";
    public static final String FIZLICO_COM = "data/fizlico_com.txt";
    public static final String JURLICO = "data/jurlico.txt";
    public static final String JURLICO_COM = "data/jurlico_com.txt";

    private static final String MSK_RESOURCE = "search/MSK.txt";
    private static final String SPB_RESOURCE = "search/SPB.txt";
    private static final String LOW_REGION_RESOURCE = "search/LOW_REGION.txt";
    private static final String HIGH_REGION_RESOURCE = "search/HIGH_REGION.txt";
    private static final String OTHER_REGION_RESOURCE = "search/OTHER_REGION.txt";

    @SuppressWarnings("unchecked")
    public static Collection<Object[]> getRegionPricesFor(String type) {
        Collection<Object[]> res = new ArrayList<>();
        getListFromFile(type)
                .forEach(result -> {
                    String[] experiment = deleteWhitespace(result).split("\\|");
                    getListFromFile(experiment[0]).forEach(rgid -> {
                        res.addAll(firstTable(rgid, experiment));
                        res.addAll(secondTable(rgid, experiment));
                        res.addAll(thirdTable(rgid, experiment));
                    });
                });
        return res;
    }

    @SuppressWarnings("unchecked")
    public static Collection<Object[]> getRegionComPricesFor(String type) {
        Collection<Object[]> res = new ArrayList<>();
        getListFromFile(type)
                .forEach(result -> {
                    String[] experiment = result.replace(" ", "").split("\\|");
                    getListFromFile(experiment[0]).forEach(rgid -> res.addAll(commercialSales(rgid, experiment)));
                });
        return res;
    }


    public static final Collection<Object[]> getExp1MSKParams() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(MSK_RESOURCE);
        List result = IOUtils.readLines(dataStream);
        result.forEach(r -> res.addAll(
                asList(new Object[][]{
                                {r, APARTMENT, SELL, __EMPTY__, 22, 749},
                                {r, HOUSE, SELL, __EMPTY__, 22, 799},
                                {r, ROOMS, SELL, __EMPTY__, 22, 699},
                                {r, LOT, SELL, __EMPTY__, 22, 299},
                                {r, GARAGE, SELL, __EMPTY__, 22, 149},
                                //second table
                                {r, APARTMENT, RENT, PER_MONTH, 22, 799},
                                {r, HOUSE, RENT, PER_MONTH, 22, 799},
                                {r, ROOMS, RENT, PER_MONTH, 22, 599},
                                {r, GARAGE, RENT, PER_MONTH, 22, 149},
                                //third table
                                {r, APARTMENT, RENT, PER_DAY, 22, 799},
                                {r, HOUSE, RENT, PER_DAY, 22, 699},
                                {r, ROOMS, RENT, PER_DAY, 22, 399},
                        }

                )));
        return res;
    }

    public static final Collection<Object[]> getExp1SPBParams() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(SPB_RESOURCE);
        List result = IOUtils.readLines(dataStream);
        result.forEach(r -> res.addAll(
                asList(new Object[][]{
                                {r, APARTMENT, SELL, __EMPTY__, 22, 749},
                                {r, HOUSE, SELL, __EMPTY__, 22, 799},
                                {r, ROOMS, SELL, __EMPTY__, 22, 799},
                                {r, LOT, SELL, __EMPTY__, 22, 299},
                                {r, GARAGE, SELL, __EMPTY__, 22, 149},
                                //second table
                                {r, APARTMENT, RENT, PER_MONTH, 22, 799},
                                {r, HOUSE, RENT, PER_MONTH, 22, 399},
                                {r, ROOMS, RENT, PER_MONTH, 22, 699},
                                {r, GARAGE, RENT, PER_MONTH, 22, 149},
                                //third table
                                {r, APARTMENT, RENT, PER_DAY, 22, 799},
                                {r, HOUSE, RENT, PER_DAY, 22, 799},
                                {r, ROOMS, RENT, PER_DAY, 22, 399},
                        }

                )));
        return res;
    }

    public static final Collection<Object[]> getExp1HightRegionParams() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(HIGH_REGION_RESOURCE);
        List result = IOUtils.readLines(dataStream);
        result.forEach(r -> res.addAll(
                asList(new Object[][]{
                                {r, APARTMENT, SELL, __EMPTY__, 22, 299},
                                {r, HOUSE, SELL, __EMPTY__, 22, 299},
                                {r, ROOMS, SELL, __EMPTY__, 22, 299},
                                {r, LOT, SELL, __EMPTY__, 22, 149},
                                {r, GARAGE, SELL, __EMPTY__, 22, 99},
                                //second table
                                {r, APARTMENT, RENT, PER_MONTH, 22, 499},
                                {r, HOUSE, RENT, PER_MONTH, 22, 299},
                                {r, ROOMS, RENT, PER_MONTH, 22, 299},
                                {r, GARAGE, RENT, PER_MONTH, 22, 99},
                                //third table
                                {r, APARTMENT, RENT, PER_DAY, 22, 499},
                                {r, HOUSE, RENT, PER_DAY, 22, 499},
                                {r, ROOMS, RENT, PER_DAY, 22, 299},
                        }

                )));
        return res;
    }

    public static final Collection<Object[]> getExp1LowRegionParams() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(LOW_REGION_RESOURCE);
        List result = IOUtils.readLines(dataStream);
        result.forEach(r -> res.addAll(
                asList(new Object[][]{
                                {r, APARTMENT, SELL, __EMPTY__, 22, 149},
                                {r, HOUSE, SELL, __EMPTY__, 22, 149},
                                {r, ROOMS, SELL, __EMPTY__, 22, 149},
                                {r, LOT, SELL, __EMPTY__, 22, 99},
                                {r, GARAGE, SELL, __EMPTY__, 22, 99},
                                //second table
                                {r, APARTMENT, RENT, PER_MONTH, 22, 149},
                                {r, HOUSE, RENT, PER_MONTH, 22, 149},
                                {r, ROOMS, RENT, PER_MONTH, 22, 149},
                                {r, GARAGE, RENT, PER_MONTH, 22, 99},
                                //third table
                                {r, APARTMENT, RENT, PER_DAY, 22, 199},
                                {r, HOUSE, RENT, PER_DAY, 22, 199},
                                {r, ROOMS, RENT, PER_DAY, 22, 199},
                        }

                )));
        return res;
    }

    public static final Collection<Object[]> getExp1OtherRegionParams() throws IOException {
        Collection<Object[]> res = new ArrayList<Object[]>();
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(OTHER_REGION_RESOURCE);
        List result = IOUtils.readLines(dataStream);
        result.forEach(r -> res.addAll(
                asList(new Object[][]{
                                {r, APARTMENT, SELL, __EMPTY__, 22, 149},
                                {r, HOUSE, SELL, __EMPTY__, 22, 149},
                                {r, ROOMS, SELL, __EMPTY__, 22, 149},
                                {r, LOT, SELL, __EMPTY__, 22, 99},
                                {r, GARAGE, SELL, __EMPTY__, 22, 99},
                                //second table
                                {r, APARTMENT, RENT, PER_MONTH, 22, 149},
                                {r, HOUSE, RENT, PER_MONTH, 22, 149},
                                {r, ROOMS, RENT, PER_MONTH, 22, 149},
                                {r, GARAGE, RENT, PER_MONTH, 22, 99},
                                //third table
                                {r, APARTMENT, RENT, PER_DAY, 22, 149},
                                {r, HOUSE, RENT, PER_DAY, 22, 149},
                                {r, ROOMS, RENT, PER_DAY, 22, 149},
                        }

                )));
        return res;
    }


    @SuppressWarnings("unchecked")
    private static List<String> getListFromFile(String path) {
        InputStream dataStream = EstimateCostTest.class.getClassLoader().getResourceAsStream(path);
        try {
            return IOUtils.readLines(dataStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cant read the file");
        }
    }

    private static List<Object[]> firstTable(String rgid, String[] data) {
        List<Object[]> table = new ArrayList<>();
        asList(APARTMENT, HOUSE, ROOMS, LOT, GARAGE).forEach(category ->
                table.add(new Object[]{rgid, category, SELL, __EMPTY__, data[1], data[2], data[3]}));
        return table;
    }

    private static List<Object[]> secondTable(String rgid, String[] data) {
        List<Object[]> table = new ArrayList<>();
        asList(APARTMENT, HOUSE, ROOMS, GARAGE).forEach(category ->
                table.add(new Object[]{rgid, category, RENT, PER_MONTH, data[1], data[2], data[3]}));
        return table;
    }

    private static List<Object[]> thirdTable(String rgid, String[] data) {
        List<Object[]> table = new ArrayList<>();
        asList(APARTMENT, HOUSE, ROOMS).forEach(category ->
                table.add(new Object[]{rgid, category, RENT, PER_DAY, data[1], data[2], data[3]}));
        return table;
    }

    private static List<Object[]> commercialSales(String rgid, String[] data) {
        return asList(new Object[][]{
                {rgid, "COMMERCIAL", SELL, __EMPTY__, data[1], data[2], data[3]},
                {rgid, "COMMERCIAL", RENT, PER_MONTH, data[1], data[2], data[3]},
                {rgid, "COMMERCIAL", RENT, PER_DAY, data[1], data[2], data[3]}
        });
    }


}
