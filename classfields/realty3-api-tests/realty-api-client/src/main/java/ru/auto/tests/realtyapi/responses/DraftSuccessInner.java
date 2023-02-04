package ru.auto.tests.realtyapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class DraftSuccessInner {
    private String status;
    private String id;
    private List<DetailsResponse> details;
}
