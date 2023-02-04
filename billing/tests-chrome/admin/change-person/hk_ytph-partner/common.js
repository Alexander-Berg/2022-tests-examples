const { processDetails, Types } = require('../helpers');

module.exports.personType = 'hk_ytph';

module.exports.partner = '1';

const details = {
    fname: { value: 'Swiss nonres partner fname', isMandatory: true },
    lname: { value: 'Swiss nonres partner lname', isMandatory: true },
    birthday: {
        type: 'date',
        value: '07.11.1990 г.',
        isMandatory: true
    },
    phone: {
        type: 'text',
        value: '12345678',
        newValue: '87654321',
        isMandatory: true
    },
    representative: 'wwwoop',
    city: { value: 'city GWV', isMandatory: true },
    postaddress: { value: 'street, 500', isMandatory: true },
    postcode: { value: '123456', isMandatory: true },
    countryId: {
        type: 'select',
        value: 'Китай',
        isMandatory: true
    },
    benAccount: { value: 'woooooooop', isMandatory: true },
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    payType: { type: Types.select, value: 'Расчетный счет', isMandatory: true },
    account: { value: 'accccount', isMandatory: true },
    swift: { value: 'SABRRUMM', isMandatory: true },
    corrSwift: 'SABRRUMM',
    paymentPurpose: { value: 'Очень деньги нужны', isMandatory: true }
};

module.exports.details = Object.values(processDetails(details));
