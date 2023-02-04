package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.model.PhotoInfo;
import ru.auto.tests.realty.vos2.model.UpdateImagesRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/offer/update_images/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateImagesByIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Vos
    private Account account;

    @Inject
    private Vos2ApiAdaptor adaptor;

    private String id;

    @Test
    public void shouldSuccessUpdateImages() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        String expectedFirstUrl = "https://avatars.mdst.yandex.net/get-realty/3220/add.15272409303933839c4ee4c/orig";
        String expectedSecondUrl = "https://avatars.mdst.yandex.net/get-realty/3022/add.1527239420650a2c1929118/orig";

        List<PhotoInfo> images = newArrayList();
        images.add(new PhotoInfo().url(expectedFirstUrl).active(true));
        images.add(new PhotoInfo().url(expectedSecondUrl).active(true));

        vos2.offer().updateImagesRoute().userIDPath(account.getId()).offerIDPath(id)
                .body(getBodyRequest().images(images)).execute(validatedWith(shouldBeStatusOk()));

        assertThat(adaptor.getUserOfferById(account.getId(), id).getPhoto().size(), equalTo(2));
        Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getPhoto().get(0)).hasUrl(expectedFirstUrl);
        Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getPhoto().get(1)).hasUrl(expectedSecondUrl);
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateImagesRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateImagesRoute().userIDPath(account.getId()).offerIDPath(id).body(new UpdateImagesRequest())
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomString();
        vos2.offer().updateImagesRoute().userIDPath(randomVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String invalidVosId = getRandomString();
        id = getRandomString();
        vos2.offer().updateImagesRoute().userIDPath(invalidVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().updateImagesRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private UpdateImagesRequest getBodyRequest() {
        UpdateImagesRequest reqBody = random(UpdateImagesRequest.class);
        return reqBody;
    }
}
