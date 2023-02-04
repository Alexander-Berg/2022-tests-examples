package ru.auto.tests.realtyapi.responses;

import lombok.Data;

@Data
public class PhoneInner {
    private String id;
    private String phone;
    private Boolean select;
}
