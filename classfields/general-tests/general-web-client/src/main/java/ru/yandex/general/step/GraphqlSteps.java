package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.Card;
import ru.yandex.general.beans.graphql.Area;
import ru.yandex.general.beans.graphql.CategorySearchRequest;
import ru.yandex.general.beans.graphql.Toponyms;

import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.general.beans.graphql.Area.area;
import static ru.yandex.general.beans.graphql.CategorySearchRequest.categorySearchRequest;
import static ru.yandex.general.beans.graphql.Request.request;
import static ru.yandex.general.beans.graphql.Toponyms.toponyms;
import static ru.yandex.general.beans.graphql.Variables.variables;

@Accessors(chain = true)
public class GraphqlSteps {

    @Inject
    private RetrofitSteps retrofitSteps;

    private static final String GET_OFFER_CARD_QUERY = "query GetOfferCard($id:String!,$categorySearchRequest:SearchRequestInput!,$hasAuth:Boolean!,$daysPeriod:Int!,$searchArea:SearchAreaInput){card(offerId:$id,searchArea:$searchArea){id title createDateTime offerVersion isOwner selfLink{canonicalUrl route}purchasableProducts @include(if:$hasAuth){...purchasableProduct}category{id shortName state forAdults searchLinks{withRequestLink(request:$categorySearchRequest){url route}}parents{id shortName state searchLinks{withRequestLink(request:$categorySearchRequest){url route}}}}description descriptionHtml photos{size_1036x1036 size_1036x1036_2x size_778x438 size_778x438_2x size_778x586 size_778x586_2x size_778x778 size_778x778_2x size_778x1036 size_778x1036_2x size_412x232 size_412x232_2x size_412x310 size_412x310_2x size_412x412 size_412x412_2x size_412x550 size_412x550_2x size_102x102 size_102x102_2x ratio preview{...photoPreview}}video{url}attributes{id name description metric value{__typename...on CardAttributeBooleanValue{booleanValue:value}...on CardAttributeDictionaryValue{dictionaryValue:key{name}}...on CardAttributeNumberValue{numberValue:value}...on CardAttributeRepeatedDictionaryValue{repeatedDictionaryValue:keys{name}}...on CardAttributeRepeatedNumberValue{repeatedNumberValue:value}...on CardAttributeRepeatedStringValue{repeatedStringValue:value}...on CardAttributeStringValue{stringValue:value}}}price{currentPrice{...offerCurrentPrice}previousRurPrice}status{type:__typename...on Inactive{timestamp reason{type:__typename...on ModerationReason{reasonText title}}}...on Banned{description{reasons{code title reason}}}}expireDateTime condition availableActions{remove edit hide activate}statistics{...offerStatistics}statisticsGraph(daysPeriod:$daysPeriod){...offerStatisticsGraph}delivery contacts{...cardContacts}seller{...seller}availableActions{remove edit hide activate}favorite @include(if:$hasAuth)note @include(if:$hasAuth){text}editFormLink{url route}indexLink{url}appliedVases{vasType vasId}deliveryInfo{selfDelivery{sendByCourier sendWithinRussia}}offerOrigin}}fragment cardContacts on CardContacts{addresses{...sellingAddressCard}preferContactWay isRedirectPhone}fragment sellingAddressCard on SellingAddress{address{address}metroStation{colors name}region{id name}district{name}geoPoint{latitude longitude}}fragment offerCurrentPrice on Price{__typename...on InCurrency{priceRur}...on Salary{salaryRur}}fragment offerStatistics on OfferStatistics{today{viewsCount favoritesCount contactsCount}total{viewsCount favoritesCount contactsCount}}fragment offerStatisticsGraph on OfferStatisticsGraph{records{date statistics{viewsCount favoritesCount contactsCount}isHighlighted}}fragment photoPreview on PhotoPreview{dataBase64 width height}fragment seller on Seller{name sellerType userBadge @optional{score}avatar{size_42x42 size_42x42_2x size_100x100 size_100x100_2x}bannedInChats publicProfileLink{url route}activeOffersCount}fragment purchasableProduct on Product{productCode priceKopecks productType}";
    private static final String DELETE_OFFER_QUERY = "mutation ($id:String!, $userId:Long!){delete(offersId: [$id], sellerId: {userId: $userId})}";

    @Step("Получаем карточку оффера «{id}»")
    public Card getOfferCard(String id) {
        return await().atMost(10, SECONDS).pollInterval(1, SECONDS)
                .until(() -> {
                    return retrofitSteps.graphqlPost(request()
                                    .setQuery(GET_OFFER_CARD_QUERY)
                                    .setVariables(variables().setId(id).setCategorySearchRequest(
                                            categorySearchRequest().setArea(area().setToponyms(
                                                    toponyms().setRegion("225"))))
                                            .setDaysPeriod(30)
                                            .setHasAuth(false)))
                            .getCard();
                }, notNullValue());
    }

    @Step("Удаляем оффер «{offerId}»")
    public void deleteOffer(String offerId, String userId) {
        retrofitSteps.graphqlPost(request()
                .setQuery(DELETE_OFFER_QUERY)
                .setVariables(variables().setId(offerId).setUserId(parseLong(userId))));
    }

}
