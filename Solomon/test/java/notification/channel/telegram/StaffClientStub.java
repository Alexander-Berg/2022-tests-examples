package ru.yandex.solomon.alert.notification.channel.telegram;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ru.yandex.misc.random.Random2;
import ru.yandex.staff.StaffClient;
import ru.yandex.staff.StaffGroup;
import ru.yandex.staff.StaffGroupMember;
import ru.yandex.staff.UserInfo;
import ru.yandex.staff.exceptions.StaffNotFoundException;


/**
 * @author alexlovkov
 **/
public class StaffClientStub implements StaffClient {

    private boolean generateUsers;
    private boolean dismissed;
    private boolean withoutTelegram;
    private boolean generateUsersByTelegramLogin;
    private final Random2 random;
    private final Map<String, UserInfo> userByTelegramLogin;


    public StaffClientStub() {
        this.random = new Random2();
        this.userByTelegramLogin = new ConcurrentHashMap<>();
        this.generateUsers = false;
        this.dismissed = false;
        this.generateUsersByTelegramLogin = false;
    }

    private UserInfo generateUser(String login) {
        return generateUser(login, getTelegramLogins(login));
    }

    private UserInfo generateUser(String login, List<String> telegramLogins) {
        return new UserInfo(
                random.nextInt(),
                login,
                "+7" + random.nextString(10, "123"),
                dismissed,
                telegramLogins,
                "",
                "");
    }

    @Override
    public CompletableFuture<UserInfo> getUserInfo(String login) {
        return CompletableFuture.supplyAsync(() -> {
            if (!generateUsers) {
                throw new StaffNotFoundException("User " + login + " not found");
            }
            return generateUser(login);
        });
    }

    @Override
    public CompletableFuture<List<UserInfo>> getUserInfo(List<String> logins) {
        return CompletableFuture.supplyAsync(() -> {
            if (!generateUsers) {
                throw new StaffNotFoundException("Users " + logins + " not found");
            }
            return logins.stream()
                    .map(this::generateUser)
                    .collect(Collectors.toList());
        });
    }

    private List<String> getTelegramLogins(String login) {
        return withoutTelegram ? Collections.emptyList() :
                List.of(login + "Telegram1", login + "Telegram2");
    }

    @Override
    public CompletableFuture<UserInfo> getUserInfoByTelegramLogin(String telegramLogin) {
        return CompletableFuture.supplyAsync(() -> {
            if (generateUsersByTelegramLogin) {
                return generateUser(telegramLogin + "@generatedStaff", List.of(telegramLogin));
            } else {
                UserInfo userInfo = userByTelegramLogin.get(telegramLogin);
                if (userInfo == null) {
                    throw new StaffNotFoundException("user not found by telegram login " + telegramLogin);
                }
                return userInfo;
            }
        });
    }

    @Override
    public CompletableFuture<List<StaffGroupMember>> getStaffGroupMembers(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<StaffGroup>> getStaffGroup(List<String> groupIds) {
        throw new UnsupportedOperationException();
    }

    void add(String telegramLogin, UserInfo userInfo) {
        userByTelegramLogin.put(telegramLogin, userInfo);
    }

    @Override
    public void close() {
    }

    public void generateUsers() {
        this.generateUsers = true;
    }

    public void doNotGenerateUsers() {
        this.generateUsers = false;
    }

    public void generateDismissedUsers() {
        this.dismissed = true;
    }

    public void generateUsersWithoutTelegram() {
        this.withoutTelegram = true;
    }

    public void generateUsersByTelegramLogin() {
        this.generateUsersByTelegramLogin = true;
    }
}
