package ru.auto.tests.realtyapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class ResponseDelete {
    private String status;
    private List<String> removed;

}
