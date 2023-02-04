package ru.yandex.auto.core.search2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.auto.core.model.CarAd;
import ru.yandex.auto.core.search2.conversion.yocto.YoctoFunctionalSubscriptionsEntityConverter;
import ru.yandex.auto.core.search2.conversion.yocto.util.Entity;

import java.util.List;

/**
 * @author andkomarov
 */
public class YoctoFunctionalSubscriptionsEntityConverterTest {
    
    @Test
    public void test() {
        YoctoFunctionalSubscriptionsEntityConverter<CarAd> converter =
                new YoctoFunctionalSubscriptionsEntityConverter<CarAd>(CarAd.class);
        CarAd item = new CarAd();
        item.setColor(new String[] {"red", "green", "yellow", "голубой металлик"});
        item.setAcceleration(1.5f);
        item.setId("1");
        item.setUrl("http://ya.ru");
        item.setYear(2030);
        List<Entity> entities = converter.asFunction().apply(item);
        // Some entities can appear from default values
        Assert.assertTrue(8 <= entities.size());
    }
}
