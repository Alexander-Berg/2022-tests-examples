package ru.auto.tests.publicapi.adaptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.utils.OfferKeeper;

import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.*;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.*;

@Singleton
public class PublicApiDealerAdaptor extends PublicApiAdaptor {
    private static final int DAYS_FORWARD = 3;
    private static final int APPLICATIONS_COUNT = 5;

    @Inject
    private OfferKeeper offerKeeper;

    @Inject
    @Prod
    private ApiClient api;

    @Step("Создаем объявление с типом {category} для дилера ({sessionId})")
    public AutoApiOffersSaveSuccessResponse createDealerOffer(String sessionId, AutoApiOffer.CategoryEnum category, String path) {
        String offerId = createDraft(sessionId, category, getResourceAsString(path)).getOfferId();
        uploadImage(getUserIdFromSession(sessionId), category, offerId);
        AutoApiOffersSaveSuccessResponse response = publishDraft(sessionId, category, offerId);
        String responseOfferId = response.getOfferId();
        offerKeeper.add(responseOfferId, category);
        return response;
    }

    @Step("Создаем объявление с типом {category} для дилера ({sessionId})")
    public AutoApiOffersSaveSuccessResponse createDealerUsedOffer(String sessionId, AutoApiOffer.CategoryEnum category) {
        OfferTemplateData data = new OfferTemplateData().withVin(getRandomVin());

        String offerId = createDraft(data, sessionId, category, "offers/cars_used_dealer.ftl").getOfferId();
        uploadImage(getUserIdFromSession(sessionId), category, offerId);
        AutoApiOffersSaveSuccessResponse response = publishDraft(sessionId, category, offerId);
        String responseOfferId = response.getOfferId();
        offerKeeper.add(responseOfferId, category);
        return response;
    }

    @Step("Создаем автостратегию для оффера {offerId}")
    public void createAutostrategiesForOffer(String sessionId, String offerId) {
        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy().fromDate(getCurrentTime())
                        .offerId(offerId).toDate(getTimeDaysForward(DAYS_FORWARD)).maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(false).forMarkModelListing(true)))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Создаем агрегатора для чата {channelName} для дилера ({sessionId})")
    public AutoApiChatAggregatorResponse createChatAggregator(String sessionId, String channelName) {
        return api.chat().createAggregator().aggregatorPath("bachata")
                .body(new AutoApiCreateChatAggregatorRequest().channelName(channelName)
                        .userPassword(getRandomString()).userEmail(getRandomEmail()).userDisplayName(getRandomString()))
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем агрегатор дилера ({sessionId})")
    public AutoApiChatAggregatorResponse getChatAggregator(String sessionId) {
        return api.chat().getAggregator().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создаем автостратегию для дилера ({sessionId}) с параметрами {mark} {model} {maxBid}")
    public void createAutoStrategy(String sessionId, String mark, String model, Long maxBid) {
        RuAutoApiAuctionChangeAuctionAutoStrategyRequest body = new RuAutoApiAuctionChangeAuctionAutoStrategyRequest()
                .context(new RuAutoApiAuctionCallAuctionRequestContext()
                        .markCode(mark)
                        .modelCode(model))
                .autoStrategy(new RuAutoApiAuctionAuctionAutoStrategySettings()
                        .maxBid(maxBid)
                        .maxPositionForPrice(new Object()));

        api.dealer().changeAuctionAutoStrategy()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(body)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем автостратегию у дилера ({sessionId}) для {mark} {model}")
    public void deleteAutoStrategy(String sessionId, String mark, String model) {
        RuAutoApiAuctionDeleteAuctionAutoStrategyRequest deleteBody = new RuAutoApiAuctionDeleteAuctionAutoStrategyRequest()
                .context(new RuAutoApiAuctionCallAuctionRequestContext()
                        .markCode(mark)
                        .modelCode(model));

        api.dealer().deleteAuctionAutoStrategy()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(deleteBody)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем текущее состояние аукциона для дилера ({sessionId})")
    public RuAutoApiAuctionAuctionStatesResponse getCurrentAuctionState(String sessionId) {
        return api.dealer().currentState()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Сделать новую ставку {newBid} в аукционе для дилера ({sessionId}) для {mark} {model}")
    public void placeBidAuction(String sessionId, String mark, String model, Long previousBid, Long newBid) {
        RuAutoApiAuctionPlaceBidRequest body = new RuAutoApiAuctionPlaceBidRequest()
                .context(new RuAutoApiAuctionCallAuctionRequestContext()
                        .markCode(mark)
                        .modelCode(model))
                .previousBid(previousBid)
                .bid(newBid);

        api.dealer().placeBid()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(body)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }
}
