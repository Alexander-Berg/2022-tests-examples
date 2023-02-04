package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.test.api.realty.OfferType;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.rules.MockRule;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.realty.rules.MockRule.DEFAULT_OFFERID;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

/**
 * Created by kopitsa on 26.07.17.
 */
public class OfferBuildingSteps {

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Inject
    private SearcherAdaptor searcherAdaptor;

    @Inject
    private RealtyWebConfig config;

    @Inject
    private WebDriverManager wm;

    private Account account;
    private List<Offer> offerList;
    private List<String> idList;
    private Offer currentOffer;
    private int count = 1;
    private boolean shouldWaitSearcher = false;
    private boolean shouldWaitVos = true;

    public OfferBuildingSteps addNewOffer(Account account) {
        this.account = account;
        offerList = newArrayList();
        idList = newArrayList();
        currentOffer = getDefaultOffer(APARTMENT_SELL);
        return this;
    }

    public OfferBuildingSteps withType(OfferType offerType) {
        currentOffer = getDefaultOffer(offerType);
        return this;
    }

    public OfferBuildingSteps withPrice(long value, String currency) {
        currentOffer.getPrice().withValue(value).withCurrency(currency);
        return this;
    }

    public OfferBuildingSteps withBody(Offer offer) {
        currentOffer = offer;
        return this;
    }

    public OfferBuildingSteps count(int count) {
        this.count = count;
        return this;
    }

    @Step("Создаём несколько объявлений")
    public OfferBuildingSteps create() {
        if (config.mockCreate() && !config.mockRecord()) {
            idList = asList(DEFAULT_OFFERID);
        } else {
            for (int i = 0; i < count; i++) {
                offerList.add(currentOffer);
            }
            idList = apiSteps.createOffers(account, offerList);
            idList.forEach(id -> MockRule.addOfferId(id));
            if (shouldWaitVos) {
                idList.forEach(id -> vos2Adaptor.waitActivateOffer(account.getId(), id));
            }
            //костыль чтобы вебдрайвер не таймаутился
            wm.getDriver();
            if (shouldWaitSearcher) {
                idList.forEach(id -> searcherAdaptor.waitOffer(id));
            }
        }
        return this;
    }

    @Step("Создаём черновик")
    public void createDefaultDraft(Account account) {
        createSpecDraft(account, asList(getDefaultDraftOffer(APARTMENT_SELL)));
    }

    @Step("Создаём черновик")
    public void createSpecDraft(Account account, List<ru.auto.test.api.realty.draft.create.userid.Offer> offers) {
        this.account = account;
        apiSteps.createDraft(account, offers);
    }

    public OfferBuildingSteps withSearcherWait() {
        shouldWaitSearcher = true;
        return this;
    }

    public OfferBuildingSteps withInactive() {
        shouldWaitVos = false;
        currentOffer.withStatus("inactive");
        return this;
    }

    public OfferBuildingSteps withBanned() {
        shouldWaitVos = false;
        currentOffer.withStatus("banned");
        return this;
    }

    public String getId() {
        return idList.get(0);
    }

    public Long getRgid() {
        return vos2Adaptor.getUserOffers(account.getId()).getOffers().get(0).getRgid();
    }

    public List<String> getIds() {
        return idList;
    }

    public static Offer getDefaultOffer(OfferType type) {
        return getObjectFromJson(Offer.class, String.format("offers/%s_offer.json", type.value()));
    }

    public static ru.auto.test.api.realty.draft.create.userid.Offer getDefaultDraftOffer(OfferType type) {
        return getObjectFromJson(ru.auto.test.api.realty.draft.create.userid.Offer.class,
                String.format("offers/%s_offer.json", type.value()));
    }
}
