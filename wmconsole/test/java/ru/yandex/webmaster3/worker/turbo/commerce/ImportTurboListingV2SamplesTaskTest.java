package ru.yandex.webmaster3.worker.turbo.commerce;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.storage.turbo.dao.commerce.model.TurboListingsInfo;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author: ishalaru
 * DATE: 28.08.2019
 */
public class ImportTurboListingV2SamplesTaskTest {
    @Test
    public void covertTest() throws IOException {
        String value = "[{\"host\":\"mycoolshop.ru\",\"verdict\":\"OK\",\"samples\":[],\"errors\":[]},{\"host\":\"mysupercoolshop.ru\",\"verdict\":\"FATAL\",\"samples\":[{\"original_url\":\"https://mysupercoolshop.ru/category/cool/\",\"turbo_url\":\"https://yandex.ru/turbo?text=listinghttps%3A%2F%2Fmysupercoolshop.ru%2Fcategory%2Fcool%2F\",\"errors\":[{\"code\":\"quality.items_mismatch\",\"severity\":\"FATAL\"}]}],\"errors\":[]},{\"host\":\"mybigshop.ru\",\"verdict\":\"WARN\",\"samples\":[],\"errors\":[{\"code\":\"bulky.categories\",\"details\":{\"count\":10000000},\"severity\":\"WARN\"}]}]";
        ObjectMapper objectMapper = new ObjectMapper();
        final TurboListingsSampleList turboAutoListingSamplesRows = objectMapper.readValue(value, TurboListingsSampleList.class);
        Assert.assertEquals("Count element",3,turboAutoListingSamplesRows.size());
    }

    public static class TurboListingsSampleList extends ArrayList<TurboListingsInfo>{

    }

}