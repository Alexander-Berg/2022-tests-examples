const elements = {
    right: '.yb-paystep__right',
    page: '.yb-user-content',
    preload: '.yb-paystep-preload',
    spinLoader: '.suggest__spin-container',
    popup: '.yb-user-popup__content',
    ignoreElements: {
        orderId: '.yb-cart-row__eid a',
        emailLabel: '.yb-paystep-main-email-label'
    },
    hideElements: {
        popupHeader: '.yb-user-popup__content header',
        submitButton: '.yb-paystep-main__pay',
        paymentTermDt: '.yb-paystep-success__payment-term-dt'
    },
    personTypes: {
        ur: '.yb-paystep-main__person-type input[value="ur"]',
        ph: '.yb-paystep-main__person-type input[value="ph"]'
    },
    mainButtons: {
        payMethod: '.yb-paystep-main__pay-method button',
        person: '.yb-paystep-main__person button',
        submit: '.yb-paystep-main-pay button',
        tumbler: '.yb-paystep-main__tumbler button',
        currency: '.yb-paystep-main__currency button',
        promocode: '.yb-paystep-main-promocode button'
    },
    menu: {
        bank: 'div[id="bank"]',
        card: 'div[id="card"]',
        yamoney: 'div[id="yamoney_wallet"]',
        webmoney: 'div[id="webmoney_wallet"]',
        paypal: 'div[id="paypal_wallet"]'
    },
    changePerson: {
        container: '.yb-paystep-main-change-person',
        personTypeButton: '.yb-paystep-main-change-person__person-type button',
        secondType: '.Popup2 div[data-key="item-1"]',
        submitButton: '.yb-paystep-main-change-person__buttons button'
    },
    modal: {
        visible: '.Modal_visible',
        view: '.yb-paystep-main-view-person',
        viewButton: '.yb-paystep-main-persons-list-person__btn_view',
        viewFormButtons: '.yb-paystep-main-view-person__buttons',
        payConfirmButton: '.yb-pay-confirmation__buttons button',
        personListItem: '.yb-paystep-main-persons-list-person_id_',
        confirmButton: '.yb-paystep-main-view-person__buttons .Button2_view_action',
        secondaryButton: '.yb-paystep-main-view-person__buttons .Button2_view_clear',
        wrapper: '.Modal-Wrapper'
    }
};

const hideElements = [
    '.b-payments__td a',
    '#lab_cur_GBP',
    elements.hideElements.popupHeader,
    elements.hideElements.submitButton
];

const ignoreElements = [
    elements.ignoreElements.orderId,
    elements.ignoreElements.emailLabel,
    'input[name="agree"]'
];

const selectorToScroll = elements.modal.wrapper;

module.exports = {
    elements,
    ignoreElements,
    hideElements,
    selectorToScroll
};
