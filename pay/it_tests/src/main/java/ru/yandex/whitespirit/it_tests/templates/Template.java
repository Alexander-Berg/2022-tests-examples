package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Template {
    CONFIGURE_GROUP_REQUEST_BODY("configure_group.json.ftlh"),
    CONFIGURE_KKT_REQUEST_BODY("configure.json.ftlh"),
    CONFIGURE_KKT_FFD12_REQUEST_BODY("configure_ffd12.json.ftlh"),
    REGISTER_KKT_REQUEST_BODY("register.json.ftlh"),
    RECEIPT_WITH_ITEM_CODE_REQUEST_BODY("receipt_with_item_code.json.ftlh"),
    SIMPLE_RECEIPTS("simple_receipts.json.flth"),
    SIMPLE_RECEIPTS_WITH_COMPOSITE_ID("simple_receipts_with_composite_id.json.flth"),
    RECEIPTS("receipts.json.flth"),
    CORRECTION_RECEIPT("correction_receipt.json.flth"),
    GET_CORRECTION_DOCUMENT("get_correction_document.json.flth"),
    RECEIPTS_WITH_CASHREGISTER_PARAMS("receipt_with_cashregister_params.ftlh"),
    RECEIPTS_CALCULATED("receipts.calculated.json.flth"),
    CLEAR_DEBUG_FN("clear_debug_fn.json.flth"),
    ;

    private final String templateName;
}
