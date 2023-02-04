package ru.auto.tests.realtyapi.responses;

import lombok.Data;

@Data
public class UserInner {
    private Boolean valid;
    private UserInnerInner user;
}
