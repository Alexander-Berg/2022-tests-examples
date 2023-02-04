const { processDetails, Types } = require('../helpers');

module.exports.personType = 'sw_ytph';

module.exports.partner = '1';

const details = {
    fname: 'Swiss nonres partner fname',
    lname: 'Swiss nonres partner lname',
    birthday: {
        type: 'date',
        value: '07.11.1990 г.'
    },
    phone: {
        type: 'text',
        value: '12345678',
        newValue: '87654321'
    },
    representative: 'wwwoop',
    city: 'city GWV',
    postaddress: 'street, 500',
    postcode: '123456',
    countryId: {
        type: 'select',
        value: 'Россия'
    },
    benAccount: 'woooooooop',
    invalidBankprops: {
        type: 'checkbox'
    },
    bankType: {
        type: 'select',
        value: 'ЮMoney'
    },
    yamoneyWallet: '123456789',
    payType: {
        type: 'select',
        value: 'Расчетный счет'
    },
    account: 'LV90HABA0117329710888',
    swift: 'SWEDSESSXXX',
    corrSwift: 'YNDMRUM1XXX',
    paymentPurpose: 'I need your money'
};

module.exports.details = processDetails(details);
