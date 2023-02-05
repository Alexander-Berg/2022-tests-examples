package com.yandex.mail.tools;

import com.yandex.mail.LoginData;

import androidx.annotation.NonNull;

public final class Accounts {

    /**
     * Team AM account
     */
    @NonNull
    public static final String TEAM_TYPE = "team";

    /**
     * Ordinary AM account
     */
    @NonNull
    public static final String LOGIN_TYPE = "login";

    /**
     * Mailish AM account
     */
    @NonNull
    public static final String MAILISH_TYPE = "external_mail";

    public static final LoginData teamLoginData = new LoginData(
            "djdonkey@yandex-team.ru",
            TEAM_TYPE,
            "111111111"
    );

    public static final LoginData testLoginData = new LoginData(
            "edcvfrtgbnh",
            LOGIN_TYPE,
            "8888888888"
    );

    public static final LoginData mailishLoginData = new LoginData(
            "mailish@gmail.com",
            MAILISH_TYPE,
            "mailish-token"
    );

    private Accounts() { }
}
