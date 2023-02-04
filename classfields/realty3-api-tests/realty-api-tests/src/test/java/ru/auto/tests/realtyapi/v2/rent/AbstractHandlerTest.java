package ru.auto.tests.realtyapi.v2.rent;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.RealtyTestApiModule;
import ru.auto.tests.realtyapi.adaptor.rent.RentApiAdaptor;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;

import java.util.HashSet;
import java.util.Set;

import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;

@Log4j
@GuiceModules(RealtyTestApiModule.class)
public abstract class AbstractHandlerTest {

    protected static Account account1;
    protected static String token1;
    protected static String uid1;

    protected static Account account2;
    protected static String token2;
    protected static String uid2;

    protected static Set<Account> accountsToDelete = new HashSet<>();
    protected static Set<RealtyRentApiFlat> flatToDelete = new HashSet<>();
    protected static Set<RealtyRentApiRentUser> usersToDelete = new HashSet<>();

    @Inject
    protected static AccountManager am;

    @Inject
    protected OAuth oAuth;

    @Inject
    protected static RentApiAdaptor rentApiAdaptor;

    protected void createAccounts() {
        log.info("Create accounts user");

        account1 = am.create();
        token1 = oAuth.getToken(account1);
        uid1 = getUid(account1);
        accountsToDelete.add(account1);

        account2 = am.create();
        token2 = oAuth.getToken(account2);
        uid2 = getUid(account2);
        accountsToDelete.add(account2);
    }

    protected static void deleteDrafts(String token) {
        for (RealtyRentApiFlat flat : flatToDelete) {
            if (flat != null && flat.getFlatId() != null) {
                String flatId = flat.getFlatId();
                log.info("Deleting created flat after test: " + flatId);
                try {
                    rentApiAdaptor.deleteFlat(token, flatId);
                    log.info("Flat draft has been deleted " + flatId);
                } catch (Throwable e) {
                    log.error("Can't delete draft " + flatId, e);
                    throw e;
                }
            }
        }
    }

    protected static void deleteUsers(String token) {
        for (RealtyRentApiRentUser user : usersToDelete) {
            if (user != null && user.getUserId() != null) {
                String userId = user.getUserId();
                log.info("Deleting created rentUser " + userId);
                try {
                    rentApiAdaptor.deleteRentUser(token, userId);
                    log.info("User has been deleted " + userId);
                } catch (Throwable e) {
                    log.info("Can't delete user " + userId, e);
                    throw e;
                }
            }
        }
    }

    protected static void deleteAccounts() {
        for (Account account : accountsToDelete) {
            try {
                if (account != null && account.getId() != null) {
                    am.delete(account.getId());
                    log.info("Account " + account.getId() + " has been deleted");
                }
            } catch (Throwable e) {
                log.error("Can't delete account", e);
                throw e;
            }
        }
    }
}
