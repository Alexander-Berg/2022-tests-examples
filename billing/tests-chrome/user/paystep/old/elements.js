const elements = {
    ignoreElements: {
        orderId: '.yb-cart-row__eid a',
        emailLabel: '.yb-paystep-main-email-label'
    },
    hideElements: {
        popupHeader: '.yb-user-popup__content header',
        submitButton: '.yb-paystep-main__pay',
        paymentTermDt: '.yb-paystep-success__payment-term-dt'
    }
};

const hideElements = [
    '.b-payments__td a',
    '#lab_cur_GBP',
    elements.hideElements.popupHeader,
    elements.hideElements.submitButton
];

const ignoreElements = [elements.ignoreElements.orderId, elements.ignoreElements.emailLabel];

module.exports = {
    ignoreElements,
    hideElements
};
