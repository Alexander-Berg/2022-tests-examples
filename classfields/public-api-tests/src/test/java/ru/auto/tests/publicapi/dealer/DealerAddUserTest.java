package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoCabinetDealerUsersList;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;
import java.util.stream.Collectors;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("POST /dealer/user")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DealerAddUserTest {

    private static final Long GROUP_ID = 8L;
    private String dealerSessionId;
    private Account userAccount;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldLinkUserToDealer() {
        String userEmail = getRandomEmail();
        userAccount = accountManager.create();
        String userSessionId = adaptor.login(userAccount).getSession().getId();
        dealerSessionId = adaptor.login(getDemoAccount()).getSession().getId();

        adaptor.addEmailToUser(userSessionId, userAccount.getId(), userEmail);

        api.dealer().linkUser().reqSpec(defaultSpec())
                .xSessionIdHeader(dealerSessionId)
                .groupQuery(GROUP_ID)
                .emailQuery(userEmail)
                .execute(validatedWith(shouldBe200OkJSON()));

        AutoCabinetDealerUsersList response = api.dealer().getUsersList().reqSpec(defaultSpec())
                .xSessionIdHeader(dealerSessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        List<String> dealerUsersIds = response.getUsers().stream().map(user -> user.getUser().getId()).collect(Collectors.toList());
        Assertions.assertThat(dealerUsersIds).contains(userAccount.getId());

        Long dealerUserGroupId = response.getUsers().stream()
                .filter(user -> user.getUser().getId().equals(userAccount.getId()))
                .findFirst().get().getAccess().getGroup().getId();
        Assertions.assertThat(dealerUserGroupId).isEqualTo(GROUP_ID);
    }

    @After
    public void unlinkUser() {
        api.dealer().unlinkUser().reqSpec(defaultSpec())
                .xSessionIdHeader(dealerSessionId)
                .userIdPath(userAccount.getId())
                .execute(validatedWith(shouldBe200OkJSON()));
    }

}
