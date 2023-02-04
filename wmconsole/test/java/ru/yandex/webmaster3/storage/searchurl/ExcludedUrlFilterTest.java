package ru.yandex.webmaster3.storage.searchurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.http.WebmasterJsonModule;
import ru.yandex.webmaster3.storage.AbstractFilter;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.BoolOpCondition;

import java.io.IOException;

/**
 * Created by ifilippov5 on 11.04.17.
 */
public class ExcludedUrlFilterTest {

    @Test
    public void deserializationTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModules(new JodaModule(), new WebmasterJsonModule(false))
                .registerModules(new ParameterNamesModule());


        String json = serialize(new ExcludedUrlFilter(ExcludedUrlSampleField.URL, AbstractFilter.Operation.TEXT_CONTAINS, "abc"));
        ExcludedUrlFilter filter = mapper.readValue(json, ExcludedUrlFilter.class);
        Assert.assertEquals("abc", filter.getValue());
        Assert.assertEquals(BoolOpCondition.Operator.AND, filter.getLogicalOperator());

        json = serialize(new ExcludedUrlFilter(ExcludedUrlSampleField.URL, AbstractFilter.Operation.TEXT_CONTAINS, "abc", BoolOpCondition.Operator.OR));
        filter = mapper.readValue(json, ExcludedUrlFilter.class);
        Assert.assertEquals("abc", filter.getValue());
        Assert.assertEquals(BoolOpCondition.Operator.OR, filter.getLogicalOperator());
    }

    private String serialize(ExcludedUrlFilter filter) throws JsonProcessingException {
        ObjectMapper OM = new ObjectMapper()
                .registerModules(new JodaModule(), new WebmasterJsonModule(false));
        return OM.writeValueAsString(filter);
    }
}
