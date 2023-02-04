package ru.auto.tests.passport.account;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

/**
 * Created by vicdev on 28.07.17.
 */
@Data
@Builder
public class Account {
    private String id;
    private String login;
    private String password;
    private String name;
    private Optional<String> phone;
}
