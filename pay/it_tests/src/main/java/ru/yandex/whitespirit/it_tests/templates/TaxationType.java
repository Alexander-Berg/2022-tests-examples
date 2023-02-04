package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaxationType {
    OSN("OSN"),
    USN_I("USN_income"),
    USN_IMC("USN_income_minus_charge"),
    ESN_CI("ESN_calc_income"),
    ESN_A("ESN_agriculture"),
    PATENT("patent");
    
    private final String value;
}
