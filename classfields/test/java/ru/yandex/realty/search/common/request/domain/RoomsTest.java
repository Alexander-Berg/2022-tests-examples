package ru.yandex.realty.search.common.request.domain;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.realty.model.offer.Rooms;

import java.util.List;

/**
 * @author aherman
 */
public class RoomsTest {
    @Test
    public void testMinimumRangeLimit() {
        List<Rooms> rooms = Cf.list(
                Rooms._1,
                Rooms.PLUS_7,
                Rooms._3,
                Rooms.PLUS_5
        );

        Rooms minimumRangeLimit = Rooms.getMinimumRoomRange(rooms);
        Assert.assertNotNull(minimumRangeLimit);
        Assert.assertEquals(Rooms.PLUS_5, minimumRangeLimit);
    }

    public void testMinimumRangeLimit_1() {
        List<Rooms> rooms = Cf.list(
                Rooms._1,
                Rooms._3
        );

        Rooms minimumRangeLimit = Rooms.getMinimumRoomRange(rooms);
        Assert.assertNull(minimumRangeLimit);
    }
}
