package ru.yandex.realty.tags;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.realty.config.RealtyTagConfig;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.JSoupSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Проверка «title» у картинок на разных страницах")
@Link("https://st.yandex-team.ru/VERTISTEST-2077")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class TitleImagesOfferTouchTest {

    private static final int INDEX_OF_CENTRAL_IMAGE = 1;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps = new JSoupSteps();

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    public RealtyTagConfig config;

    @Before
    public void before() {
        MockOffer offer = mockOffer(SELL_APARTMENT);
        String port = mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).withDefaults().create().getPort();
        String offerId = offer.getOfferId();
        String host = config.getTestingURI().toString();
        host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        jSoupSteps.connectTo(format("%s/offer/%s/", host, offerId)).cookie("mockritsa_imposter", port).mobileHeader()
                .get();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeTitleInImgInOffer() {
        final String elementSelector = ".SwipeGallery__thumb-img";
        final String titleExpected = "Купить 3-комнатную квартиру 85 м², 3/17 этаж - Яндекс Недвижимость";
        String titleActual = jSoupSteps.select(elementSelector).first().attr("title");
        assertThat(titleActual).isEqualTo(titleExpected);
    }
}
