const elements = {
    page: '.yb-user-content',
    addEmptyPersons: '.yb-empty-persons__add-button',
    editPerson: '.yb-persons-person-title__button-edit',
    backButton: '.yb-persons-person__back',
    personsType: {
        radioButton: {
            ur: 'input[value="ur"]',
            ph: 'input[value="ph"]'
        },
        listBox: 'button[role="listbox"]'
    },
    continueButton: '.yb-change-person-category-field__buttons button',
    submitButton:
        '.src-common-modules-change-person-components-ChangePerson-___styles-module__buttons .yb-presentional-button',
    error: '.src-common-presentational-components-Field-___field-module__detail__error',
    usStateButton: '[data-detail-id="usState"]',
    name: '[name="name"]',
    phone: '[name="phone"]',
    email: '[name="email"]',
    city: '[name="city"]',
    postaddress: '[name="postaddress"]',
    postcode: '[name="postcode"]',
    file: '[type="file"]',
    payType: {
        button: '[data-detail-id="payType"] button',
        nothing: 'span=не выбрано',
        iban: {
            list: 'span=IBAN',
            input: '[name="iban"]'
        },
        account: {
            list: 'span=Расчетный счет',
            input: '[name="account"]'
        },
        other: {
            list: 'span=Прочее',
            input: '[name="other"]'
        }
    },
    swift: '[name="swift"]',
    corr_swift: '[name="corr-swift"]',
    ben_bank: '[name="ben-bank"]',
    ben_bank_code: '[name="ben-bank-code"]',
    longname: '[name="longname"]',
    fax: '[name="fax"]',
    countryIdButton: '[data-detail-id="countryId"] button',
    representative: '[name="representative"]',
    signer_person_name: '[name="signer-person-name"]',
    signerPositionNameButton: '[data-detail-id="signerPositionName"] button',
    purchase_order: '[name="purchase-order"]',
    legaladdress: '[data-detail-id="legaladdress"] textarea',
    legalAddressStreet: '[name="legal-address-street"]',
    legalAddressPostcode: '[name="legal-address-postcode"]',
    legalAddressHome: '[name="legal-address-home"]',
    isSamePostaddress: '[name="is-same-postaddress"]',
    inn: '[name="inn"]',
    fname: '[name="fname"]',
    lname: '[name="lname"]',
    mname: '[name="mname"]',
    authorityDocType: '[data-detail-id="authorityDocType"]',
    organization: '[name="organization"]',
    agree: '[name="agree"]',
    kz_in: '[name="kz-in"]',
    tva_number: '[name="tva-number"]',
    vat_number: '[name="vat-number"]',
    account: '[name="account"]',
    legaladdressInput: '[name="legaladdress"]',
    rnn: '[name="rnn"]',
    kbe: '[name="kbe"]',
    iik: '[name="iik"]',
    bik: '[name="bik"]',
    bank: '[name="bank"]'
};

const ignoreElements = [
    'div[data-detail-id="envelopeAddress"] textarea',
    'div[data-detail-id="legalSample"] textarea',
    'textarea[name="legaladdress"]'
];

module.exports = {
    elements,
    ignoreElements
};
