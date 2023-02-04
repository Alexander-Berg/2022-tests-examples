package ru.auto.tests.realtyapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class IdsInner {
    private String status;
    private List<String> ids;
}
