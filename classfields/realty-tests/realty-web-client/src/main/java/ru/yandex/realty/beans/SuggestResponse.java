package ru.yandex.realty.beans;

import lombok.Getter;

import java.util.List;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
public class SuggestResponse {
    private List<SuggestItem> result;
}
