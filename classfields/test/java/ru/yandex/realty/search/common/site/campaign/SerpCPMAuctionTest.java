package ru.yandex.realty.search.common.site.campaign;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.model.billing.Campaign;
import ru.yandex.realty.model.sites.Company;
import ru.yandex.realty.sites.auction.AuctionFullRunner;
import ru.yandex.realty.sites.campaign.auction.AuctionFullResult;
import ru.yandex.realty.sites.campaign.auction.BidInfo;
import ru.yandex.realty.sites.campaign.auction.BidPriority;

import java.util.*;

/**
 * Created by abulychev on 17.08.15.
 */
public class SerpCPMAuctionTest {
    private static final Long SITE_A = 11L, SITE_B = 12L, SITE_C = 13L;

    private static final Company ADVECS = new Company(1), MIEL = new Company(2);
    private static final Company EST_A_TET = new Company(3), CNN = new Company(4);

    private static Campaign newCampaign(String id) {
        return new Campaign(id, 0, 0, "0", new HashMap<>(), Collections.emptyList(), Collections.emptyList(),0, 0, false, false, 0, 0L, null, new HashMap<>(), null, null);
    }

    private static final Campaign CAMPAIGN_1 = newCampaign("1"), CAMPAIGN_2 = newCampaign("2");
    private static final Campaign CAMPAIGN_3 = newCampaign("3"), CAMPAIGN_4 = newCampaign("4");
    private static final Campaign CAMPAIGN_5 = newCampaign("5"), CAMPAIGN_6 = newCampaign("6");
    private static final Campaign CAMPAIGN_7 = newCampaign("7"), CAMPAIGN_8 = newCampaign("8");

    private static final Comparator<Long> comparator =
            new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    return Long.compare(o1, o2);
                }
            };

//  Examples were taken from https://beta.wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-cpm-auction/

    private static void submitBid(Map<Long, List<BidInfo<Long>>> entity2bids, Long entity, Object company, Object campaign, long bid, long factor, boolean isWorking) {
        BidPriority bp = isWorking ? BidPriority.ACTIVE_WORKING : BidPriority.ACTIVE_NOT_WORKING;
        BidInfo<Long> bi = new BidInfo<>(entity, (Company)company, (Campaign)campaign, bid, factor, bp);
        List<BidInfo<Long>> values = entity2bids.get(entity);
        if (values == null)
        {
            values = new ArrayList<>();
            entity2bids.put(entity, values);
        }
        values.add(bi);
    }

    @Test
    public void test1() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 5000, 100, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 4000, 100, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 3000, 100, true);

        submitBid(entity2bids, SITE_B, ADVECS, CAMPAIGN_4, 3500, 100, true);
        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_5, 3000, 100, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_6, 2000, 100, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(ADVECS, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(0));
        Assert.assertEquals(5000L, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(ADVECS, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_4, result.getCampaign(1));
        Assert.assertEquals(3500L, result.getRevenue(1));
    }

    @Test
    public void test2() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 5000, 130, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 4000, 130, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 3000, 130, true);

        submitBid(entity2bids, SITE_B, ADVECS, CAMPAIGN_4, 3000, 200, true);
        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_5, 3500, 200, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_6, 2000, 200, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_B, result.getEntity(0));
        Assert.assertEquals(MIEL, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_5, result.getCampaign(0));
        Assert.assertEquals(3500L, result.getRevenue(0));

        Assert.assertEquals(SITE_A, result.getEntity(1));
        Assert.assertEquals(ADVECS, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(1));
        Assert.assertEquals(5000L, result.getRevenue(1));
    }

    @Test
    public void test3() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 5000, 100, true);
        submitBid(entity2bids, SITE_B, ADVECS, CAMPAIGN_2, 4000, 100, true);
        submitBid(entity2bids, SITE_C, ADVECS, CAMPAIGN_3, 3000, 100, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(ADVECS, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(0));
        Assert.assertEquals(5000L, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(ADVECS, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_2, result.getCampaign(1));
        Assert.assertEquals(4000L, result.getRevenue(1));

        Assert.assertEquals(SITE_C, result.getEntity(2));
        Assert.assertEquals(ADVECS, result.getCompany(2));
        Assert.assertEquals(CAMPAIGN_3, result.getCampaign(2));
        Assert.assertEquals(3000L, result.getRevenue(2));
    }

    @Test
    public void test4() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 600, 400, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 600, 400, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 600, 400, true);

        submitBid(entity2bids, SITE_B, ADVECS, CAMPAIGN_4, 600, 300, true);
        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_5, 600, 300, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_6, 600, 300, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(600L, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(600L, result.getRevenue(1));
    }

    @Test
    public void test5() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 750, 600, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 600, 600, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 600, 600, true);

        submitBid(entity2bids, SITE_B, ADVECS, CAMPAIGN_4, 750, 500, true);
        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_5, 600, 500, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_6, 600, 500, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(ADVECS, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(0));
        Assert.assertEquals(750L, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(ADVECS, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_4, result.getCampaign(1));
        Assert.assertEquals(750L, result.getRevenue(1));
    }

    @Test
    public void test6() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 75000, 550, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 60000, 550, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 60000, 550, true);

        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_4, 60000, 500, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_5, 60000, 500, true);
        submitBid(entity2bids, SITE_B, CNN, CAMPAIGN_6, 70000, 500, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(ADVECS, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(0));
        Assert.assertEquals(75000L, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(CNN, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_6, result.getCampaign(1));
        Assert.assertEquals(70000L, result.getRevenue(1));
    }

    @Test
    public void test7() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 75000, 400, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 60000, 400, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 60000, 400, true);

        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_4, 60000, 400, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_5, 60000, 400, true);
        submitBid(entity2bids, SITE_B, CNN, CAMPAIGN_6, 70000, 400, true);

        submitBid(entity2bids, SITE_C, EST_A_TET, CAMPAIGN_7, 85000, 400, true);
        submitBid(entity2bids, SITE_C, MIEL, CAMPAIGN_8, 60000, 400, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_C, result.getEntity(0));
        Assert.assertEquals(EST_A_TET, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_7, result.getCampaign(0));
        Assert.assertEquals(85000, result.getRevenue(0));

        Assert.assertEquals(SITE_A, result.getEntity(1));
        Assert.assertEquals(ADVECS, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(1));
        Assert.assertEquals(75000, result.getRevenue(1));

        Assert.assertEquals(SITE_B, result.getEntity(2));
        Assert.assertEquals(CNN, result.getCompany(2));
        Assert.assertEquals(CAMPAIGN_6, result.getCampaign(2));
        Assert.assertEquals(70000, result.getRevenue(2));
    }

    @Test
    public void test8() {
        Map<Long, List<BidInfo<Long>>> entity2bids = new LinkedHashMap<>();

        submitBid(entity2bids, SITE_A, ADVECS, CAMPAIGN_1, 75000, 400, true);
        submitBid(entity2bids, SITE_A, MIEL, CAMPAIGN_2, 60000, 400, true);
        submitBid(entity2bids, SITE_A, EST_A_TET, CAMPAIGN_3, 60000, 400, true);

        submitBid(entity2bids, SITE_B, MIEL, CAMPAIGN_4, 60000, 400, true);
        submitBid(entity2bids, SITE_B, EST_A_TET, CAMPAIGN_5, 60000, 400, true);
        submitBid(entity2bids, SITE_B, CNN, CAMPAIGN_6, 70000, 400, true);

        submitBid(entity2bids, SITE_C, EST_A_TET, CAMPAIGN_7, 85000, 200, true);
        submitBid(entity2bids, SITE_C, MIEL, CAMPAIGN_8, 80000, 200, true);

        AuctionFullResult<Long> result = AuctionFullRunner.createAuctionFullResult(entity2bids, comparator);

        Assert.assertEquals(SITE_A, result.getEntity(0));
        Assert.assertEquals(ADVECS, result.getCompany(0));
        Assert.assertEquals(CAMPAIGN_1, result.getCampaign(0));
        Assert.assertEquals(75000, result.getRevenue(0));

        Assert.assertEquals(SITE_B, result.getEntity(1));
        Assert.assertEquals(CNN, result.getCompany(1));
        Assert.assertEquals(CAMPAIGN_6, result.getCampaign(1));
        Assert.assertEquals(70000, result.getRevenue(1));

        Assert.assertEquals(SITE_C, result.getEntity(2));
        Assert.assertEquals(EST_A_TET, result.getCompany(2));
        Assert.assertEquals(CAMPAIGN_7, result.getCampaign(2));
        Assert.assertEquals(85000, result.getRevenue(2));
    }
}
