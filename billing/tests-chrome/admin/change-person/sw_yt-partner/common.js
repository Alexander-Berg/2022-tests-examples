const { processDetails, Types } = require('../helpers');

module.exports.personType = 'sw_yt';

module.exports.partner = '1';

const details = {
    name: {
        type: 'text',
        value: 'Swiss nonres legal Old Payer',
        newValue: 'Swiss nonres legal New Payer'
    },
    longname: 'Swiss nonres legal Long Payer',
    phone: '+41 32 3446399',
    fax: '+41 32 1599850',
    email: 'abc@def.ghi',
    representative: 'wwwoop',
    signerPersonName: 'Innnnnokenty',
    signerPositionName: {
        type: Types.select,
        value: 'Президент'
    },
    authorityDocType: {
        type: 'select',
        value: 'Устав'
    },
    city: 'city GWV',
    postaddress: 'street, 500',
    postcode: '123456',
    countryId: {
        type: 'select',
        value: 'Россия',
        scrollIntoView: true
    },
    legaladdress: 'adf',
    inn: 'asdf',
    invalidBankprops: {
        type: 'checkbox'
    },
    payType: {
        type: 'select',
        value: 'Расчетный счет'
    },
    account: 'LV90HABA0117329710888',
    swift: 'SABRRUMM',
    corrSwift: 'SABRRUMM',
    paymentPurpose: 'I need your money'
};

module.exports.details = processDetails(details);
