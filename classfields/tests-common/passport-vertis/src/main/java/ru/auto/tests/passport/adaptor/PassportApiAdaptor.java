package ru.auto.tests.passport.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import retrofit2.Response;
import ru.auto.test.passport.ApiClient;
import ru.auto.test.passport.api.AuthorizationApi;
import ru.auto.test.passport.api.ClientsApi;
import ru.auto.test.passport.api.ConfirmationApi;
import ru.auto.test.passport.api.ModerationApi;
import ru.auto.test.passport.api.UserApi;
import ru.auto.test.passport.api.UsersApi;
import ru.auto.test.passport.api.UsersIdentitiesApi;
import ru.auto.test.passport.model.AutoruUserProfile;
import ru.auto.test.passport.model.ChangeEmailParameters;
import ru.auto.test.passport.model.ConfirmIdentityResult;
import ru.auto.test.passport.model.ConfirmationCode;
import ru.auto.test.passport.model.CreateUserResult;
import ru.auto.test.passport.model.Event;
import ru.auto.test.passport.model.LoginParameters;
import ru.auto.test.passport.model.LoginResult;
import ru.auto.test.passport.model.RequestEmailChangeParameters;
import ru.auto.test.passport.model.SmsLogRecord;
import ru.auto.test.passport.model.UserEssentials;
import ru.auto.test.passport.model.UserIdentity;
import ru.auto.test.passport.model.UserProfile;
import ru.auto.test.passport.model.UserSource;
import ru.auto.tests.passport.api.CustomModerationApi;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by vicdev on 28.02.17.
 */
public class PassportApiAdaptor extends AbstractModule {

    @Inject
    private ApiClient passport;

    public static final String SERVICE = "auto";
    public static final String DEFAULT_PASSWORD = "autoru";

    @Step("Подтверждаем email ({email}) кодом ({code})")
    public Response<ConfirmIdentityResult> confirmEmail(String code, String email) {
        try {
            return passport.createService(ConfirmationApi.class).confirmAddEmail(SERVICE, email, code, null, null,
                    null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Создаем аккаунт, привязанный к почте {email}, с паролем {pass}")
    public Response<CreateUserResult> createAccountWithoutConfirmationByEmail(String email, String pass) {
        try {
            return passport.createService(UsersApi.class)
                    .createUser(SERVICE, new UserSource().profile(new UserProfile().autoru(new AutoruUserProfile()))
                            .email(email)
                            .password(pass)
                            .skipNotifications(true),
                            null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //without step
    public void deleteAccount(String uid) {
        try {
            passport.createService(UserApi.class).deleteUser(uid, SERVICE, true,
                    null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Step("Подтверждаем телефон {phone} кодом ({code})")
    public Response<ConfirmIdentityResult> confirmPhone(String code, String phone) {
        try {
            return passport.createService(ConfirmationApi.class).confirmPhone(SERVICE, phone, code, null,
                    null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Создаем аккаунт, привязанный к телефону {phone}, с паролем {pass}")
    public Response<CreateUserResult> createAccountWithoutConfirmationByPhone(String phone, String pass) {
        try {
            return passport.createService(UsersApi.class)
                    .createUser(SERVICE, new UserSource().profile(new UserProfile().autoru(new AutoruUserProfile()))
                            .phone(phone)
                            .password(pass)
                            .skipNotifications(true),
                            null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Запрашиваем смену емейла для пользователя {uid}")
    public void requestEmailChangeForUserWithPhone(String uid, String phone) {
        try {
            passport.createService(UsersIdentitiesApi.class)
                    .requestEmailChange(uid, SERVICE,
                            null, null, null, null, null, null, null, null, null, null, null,
                            new RequestEmailChangeParameters().currentIdentity(new UserIdentity().phone(phone)))
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Меняем емейл пользователю {uid} на {email}")
    public void changeEmailForUserWithPhone(String uid, String email, String phone, String smsCode) {
        try {
            passport.createService(UsersIdentitiesApi.class)
                    .changeEmail(uid, SERVICE, null, null, null, null, null, null, null, null, null, null, null,
                            new ChangeEmailParameters().email(email)
                                    .confirmationCode(new ConfirmationCode().code(smsCode).identity(new UserIdentity().phone(phone))))
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Получаем essentials пользователя {uid}")
    public Response<UserEssentials> getUserEssentials(String uid) {
        try {
            return passport.createService(UserApi.class)
                    .getUserEssentials(uid, SERVICE, false,
                            null, null, null, null, null, null, null, null, null, null, null)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Добавляем почту пользователю {uid}")
    public void addEmailToAccountForUserWithPhone(String uid, String phone, String email) {
        requestEmailChangeForUserWithPhone(uid, phone);

        String smsCode = getSmsCode(uid, 0);
        changeEmailForUserWithPhone(uid, email, phone, smsCode);

        String emailCode = getEmailCode(uid, 0);
        confirmEmail(emailCode, email);
    }

    @Step("Привязываем пользователя {uid} к клиенту {clientId}")
    public void linkUserToClient(String uid, String clientId, String clientGroupId) {
        try {
            passport.createService(ClientsApi.class)
                    .linkToClient(clientId, SERVICE, uid, clientGroupId,
                            null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Отвязываем пользователя {uid} от клиента")
    public void unlinkUserFromClient(String uid) {
        try {
            passport.createService(ClientsApi.class)
                    .unlinkFromClient(uid, SERVICE,
                            null, null, null, null, null, null, null, null, null, null, null).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Получаем смс код отправленный пользователю {uid}")
    public String getSmsCode(String uid, int indexOfSms) {
        List<Event> eventsList = await().ignoreExceptions().atMost(10, SECONDS).pollInterval(1, SECONDS)
                .until(() -> passport.createService(ModerationApi.class)
                        .getEvents(SERVICE, uid, "SMS_SENT",
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                        .execute().body().getEvents(), notNullValue());

        String eventText = eventsList.get(indexOfSms).getPayload().getSmsSent().getText();

        Pattern codePattern = Pattern.compile("\\d+");
        Matcher m = codePattern.matcher(eventText);
        if (m.find()) {
            return m.group();
        } else return null;
    }

    @Step("Получаем последний смс код, отправленный на телефон {phone}")
    public String getLastSmsCode(String phone) {
        try {
            List<SmsLogRecord> smsLogs = passport.createService(CustomModerationApi.class)
                    .getSmsLogs(SERVICE, phone, 1)
                    .execute().body();

            String eventText = smsLogs.get(0).getOutgoing();

            Pattern codePattern = Pattern.compile("\\d+");
            Matcher m = codePattern.matcher(eventText);

            if (m.find()) {
                return m.group();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Получаем код отправленный на email юзера {uid}")
    public String getEmailCode(String uid, int indexOfEmail) {
        List<Event> eventsList = await().ignoreExceptions().atMost(10, SECONDS).pollInterval(1, SECONDS)
                .until(() -> passport.createService(ModerationApi.class)
                        .getEvents(SERVICE, uid, "EMAIL_SENT",
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                        .execute().body().getEvents(), notNullValue());

        return eventsList.get(indexOfEmail).getPayload().getEmailSent().getArguments().get("code");
    }

    @Step("Авторизуемся {login}")
    public LoginResult login(String login, String password) {
        passport.getOkBuilder().connectTimeout(30, SECONDS);
        try {
            return passport.createService(AuthorizationApi.class)
                .login(SERVICE, null, null, null, null, null, null, null, null, null, null,
                null, new LoginParameters().login(login).password(password)).execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configure() {
    }
}
