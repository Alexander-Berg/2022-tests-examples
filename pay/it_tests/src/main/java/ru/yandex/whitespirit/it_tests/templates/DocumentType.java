package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {
    REGISTRATION("RegistrationReport"),
    RE_REGISTRATION("ReRegistrationReport"),
    SHIFT_OPEN("ShiftOpenReport"),
    SHIFT_CLOSE("ShiftCloseReport"),
    RECEIPT("Receipt"),
    BSO("BSO"),
    CORRECTION_RECEIPT("CorrectionReceipt"),
    CORRECTION_BSO("CorrectionBSO"),
    CLOSE_FN("CloseFNReport"),
    CURRENT("CurrentReport");

    private final String value;
}
