package ru.yandex.realty.storage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.realty.model.offer.PhoneNumber;
import ru.yandex.realty.model.ban.PartnerBanInfo;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.SaleAgent;

import java.util.*;

/**
 * User: tolmach
 * Date: 15.07.16.
 */
public class Partner2BanStorageTest {
    private Partner2BanStorage storage;

    private List<PartnerBanInfo> partnerBanInfoList = new ArrayList<>();

    @Before
    public void initialize() {
        long curPartnerId = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                partnerBanInfoList.add(new PartnerBanInfo(curPartnerId++,
                        genAgencies(i),
                        genPhones(j)));
            }
        }
        storage = new Partner2BanStorage(partnerBanInfoList);
    }

    public Set<String> genAgencies(int count) {
        Set<String> agencies = new HashSet<>();
        for (int i = 0; i < count; i++) {
            agencies.add(Integer.toString(i));
        }
        return agencies;
    }

    public Set<PhoneNumber> genPhones(int count) {
        Set<PhoneNumber> phones = new HashSet<>();
        int mainNumbs = 8000000;
        for (int i = 0; i < count; i++) {
            phones.add(new PhoneNumber("925", Integer.toString(mainNumbs)));
        }
        return phones;
    }

    @Test
    public void test() {
        String fakeAgency = Integer.toString(6000);
        PhoneNumber fakeNumber = new PhoneNumber("(925)", Integer.toString(8800000));
        long fakePartner = 6000L;


        for (PartnerBanInfo info : partnerBanInfoList) {
            for (String agencyId : info.getAgencies()) {
                for (PhoneNumber number : info.getPhones()) {
                    testByParams(info.getPartnerId(), agencyId, number, true);
                    testByParams(info.getPartnerId(), agencyId, fakeNumber, true);
                    testByParams(info.getPartnerId(), fakeAgency, number, true);
                    testByParams(fakePartner, agencyId, number, false);
                    testByParams(fakePartner, fakeAgency, fakeNumber, false);
                }
            }
        }
    }

    public void testByParams(long partnerId, String agencyId, PhoneNumber number, boolean exp) {
        customAssert(partnerId, agencyId, number, " params" + (exp ?"":" not"), storage.isOfferBan(partnerId, agencyId, number) == exp);

        Offer testOffer = new Offer();
        testOffer.setPartnerId(partnerId);
        SaleAgent testAgent = testOffer.createAndGetSaleAgent();
        testAgent.setAgencyId(agencyId);
        testAgent.setUnifiedPhones(Collections.singletonList(number));
        customAssert(partnerId, agencyId, number, " offer" + (exp ?"":" not"), storage.isOfferBan(testOffer) == exp);
    }

    public void customAssert(long partnerId, String agencyId, PhoneNumber number, String word, boolean expr) {
        Assert.assertTrue(genDebugInfo(partnerId, agencyId, number, word), expr);
    }

    public String genDebugInfo(long partnerId, String agencyId, PhoneNumber number, String word) {
        return String.format("partnerId = %d, agencyId = %s, phoneNumber = %s" + word + " banned", partnerId, agencyId, number.toString());
    }
}
