package ru.yandex.realty.searcher.response.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.yandex.realty.newbuilding.NewbuildingOffer;

@RunWith(JUnit4.class)
public class OffersCountPerHouseResponseTest {

    @Test
    public void testOffersCountPerHouseResponseEmpty() throws Exception {
        OffersCountPerHouseResponse.Builder builder = new OffersCountPerHouseResponse.Builder();
        OffersCountPerHouseResponse empty = builder.build();
        Assert.assertArrayEquals("empty items expected", new OffersCountPerHouseResponse.Entry[0], empty.getItems().toArray());
        Assert.assertEquals("zero total count expected", 0, empty.getTotalOffersCount().intValue());
        Assert.assertEquals("zero unmatched count expected", 0, empty.getUnmatchedOffersCount().intValue());
    }

    @Test
    public void testOffersCountPerHouseResponseGenericUsage() throws Exception {
        OffersCountPerHouseResponse.Builder builder = new OffersCountPerHouseResponse.Builder();

        builder.withNewbuildingOfferCounted(getNewbuildingOfferBuilder(1931).build());        // no houseId
        builder.withNewbuildingOfferCounted(withHouseId(getNewbuildingOfferBuilder(1922), 17L).build());
        builder.withNewbuildingOfferCounted(withHouseId(getNewbuildingOfferBuilder(1923), 42L).build());
        builder.withNewbuildingOfferCounted(withHouseId(getNewbuildingOfferBuilder(1024), 17L).build());

        OffersCountPerHouseResponse response = builder.build();
        Assert.assertEquals(4, response.getTotalOffersCount().intValue());
        Assert.assertEquals(1, response.getUnmatchedOffersCount().intValue());
        Assert.assertEquals(
                ImmutableSet.builder()
                        .add(OffersCountPerHouseResponse.Entry.of("17", 2))
                        .add(OffersCountPerHouseResponse.Entry.of("42", 1))
                        .build(),
                response.getItems());

        Assert.assertTrue(response.getItems().iterator().next().getOffersCount() > 0);

        ObjectMapper om = new ObjectMapper();
        System.out.println(om.writeValueAsString(response));
    }

    @NotNull
    private NewbuildingOffer.Builder getNewbuildingOfferBuilder(int i) {
        NewbuildingOffer.Builder result = new NewbuildingOffer.Builder(i);
        result.primarySaleV2 = false;       // extinguish some of its internal checks (not relevant for the test)
        return result;
    }

    @NotNull
    private static NewbuildingOffer.Builder withHouseId(NewbuildingOffer.Builder builder, Long houseId) {
        builder.houseId = houseId;
        return builder;
    }

}
