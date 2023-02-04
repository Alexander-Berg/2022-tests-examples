package ru.auto.tests.publicapi.rules;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

public class DeleteAggregatorsRule extends ExternalResource {
  private static final Logger log = Logger.getLogger(DeleteAggregatorsRule.class);

  @Inject
  @Prod
  private ApiClient api;

  @Inject
  private AccountKeeper accountKeeper;

  @Inject
  private PublicApiAdaptor adaptor;

  protected void after() {
    this.accountKeeper.get().forEach(this::deleteAggregators);
  }

  void deleteAggregators (Account account){
    try {
    String sessionId = adaptor.login(account).getSession().getId();

    api.chat().deleteAggregator()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .executeAs(validatedWith(shouldBe200OkJSON()));
    } catch (Throwable e) {
      log.error(String.format("Can't delete binded aggregators for uid %s", account.getId()), e);
    }
  }

}
