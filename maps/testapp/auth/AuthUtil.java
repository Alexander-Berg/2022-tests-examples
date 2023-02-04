package com.yandex.maps.testapp.auth;

import androidx.annotation.NonNull;

import com.yandex.mrc.ride.MRCFactory;
import com.yandex.passport.api.Passport;
import com.yandex.passport.api.PassportAccount;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.maps.auth.AccountFactory;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.passport.api.PassportApi;
import com.yandex.passport.api.PassportFilter;
import com.yandex.passport.api.PassportLoginProperties;
import com.yandex.passport.api.PassportToken;
import com.yandex.passport.api.PassportUid;
import com.yandex.passport.api.PassportVisualProperties;
import com.yandex.runtime.auth.Account;

import java.util.HashMap;
import java.util.Map;

/**
 * This util class manipulates status of user
 * login in the application.
 */
public final class AuthUtil {
    public static final int GET_ACCOUNTS = 0xBEBE;
    public static final int REQUEST_CODE_RELOGIN = 0xBEBC;
    public static PassportApi passportApi_ = null;

    public static final PassportFilter defaultFilter = PassportFilter.Builder.Factory.createBuilder()
        .setPrimaryEnvironment(Passport.PASSPORT_ENVIRONMENT_PRODUCTION)
        .build();

    private static PassportAccount passportAccount = null;
    private static HashMap<PassportUid, PassportToken> authorized = new HashMap<>();

    /**
     * Sets account of the application.
     */
    public static void setCurrentAccount(PassportAccount account) {
        if (account != null && !hasToken(account))
            return;

        passportAccount = account;
        Account mapkitAccount = account != null ? AccountFactory.createAccount(account) : null;
        MapKitFactory.getInstance().setAccount(mapkitAccount);
        RecordingFactory.getInstance().setAccount(mapkitAccount);
        MRCFactory.getInstance().setAccount(mapkitAccount);
    }

    /**
     * Returns current account of the application.
     *
     * @return Account if set and still in the system and null otherwise.
     */
    public static PassportAccount getCurrentAccount() {
        return passportAccount;
    }

    public static PassportToken getCurrentAccountToken() {
        return passportAccount != null && authorized.containsKey(passportAccount.getUid()) ? authorized.get(passportAccount.getUid()) : null;
    }

    public static PassportLoginProperties createPassportLoginProperties() {
        return Passport.createPassportLoginPropertiesBuilder()
            .requireAdditionOnly()
            .setFilter(AuthUtil.defaultFilter)
            .setVisualProperties(
                PassportVisualProperties.Builder.Factory.create()
                    .disableSocialAuthorization()
                    .build())
            .build();
    }

    public static boolean hasToken(@NonNull PassportAccount account) {
        return authorized.containsKey(account.getUid());
    }

    public static void setToken(@NonNull PassportAccount account, PassportToken token) {
        authorized.put(account.getUid(), token);
        setCurrentAccount(account);
    }

    public static void removeToken(@NonNull String token) {
        PassportUid uid = null;
        for(Map.Entry<PassportUid, PassportToken> e : authorized.entrySet()) {
            if (e.getValue().getValue().equals(token))
                uid = e.getKey();
        }

        if (uid != null) {
            authorized.remove(uid);
            removeCurrentAccount(uid);
        }
    }

    public static void removeAccount(@NonNull PassportUid uid) {
        authorized.remove(uid);
        removeCurrentAccount(uid);
    }

    private static void removeCurrentAccount(@NonNull PassportUid uid) {
        if (passportAccount != null && passportAccount.getUid().equals(uid))
            setCurrentAccount(null);
    }
}
