package ru.auto.tests.realtyapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class UserInnerInner {
    private String name;
    private String status;
    private List<PhoneInner> phones;
    private String email;

}
