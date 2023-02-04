package ru.yandex.webmaster3.storage.iks;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.webmaster3.storage.iks.util.IksTableUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ifilippov5 on 22.04.18.
 */
@Ignore
public class IksTableUtilTest {

    @Test
    public void createPredicateByDateTest() {
        List<String> names = new ArrayList<>();
        names.add("iks_1520413440706_distrib");
        names.add("iks_1520413440706_merge");
        names.add("iks_1520413440706_part_0");
        names.add("iks_1500313440706_merge");

        List<Pair<LocalDate, LocalDate>> dates = new ArrayList<>();
        dates.add(Pair.of(new LocalDate("2018-03-07"), new LocalDate("2018-03-07")));
        dates.add(Pair.of(new LocalDate("2018-03-08"), new LocalDate("2018-03-07")));
        dates.add(Pair.of(new LocalDate("2017-07-16"), new LocalDate("2018-03-06")));
        dates.add(Pair.of(new LocalDate("2017-07-16"), new LocalDate("2018-03-07")));
        dates.add(Pair.of(new LocalDate("2018-01-01"), new LocalDate("2018-03-07")));
        List<Integer> expectedSize = Arrays.asList(1, 0, 1, 2, 1);
        Iterator<Integer> it = expectedSize.iterator();
        dates.forEach(pair -> {
            int size = names.stream()
                    .filter(IksTableUtil.createPredicateByNameAndDate(pair.getLeft(), pair.getRight()))
                    .collect(Collectors.toList())
                    .size();
            Assert.assertEquals((int)it.next(), size);
        });
    }
}
