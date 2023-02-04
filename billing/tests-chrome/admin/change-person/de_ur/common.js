const { processDetails, Types } = require('../helpers');

module.exports.personType = 'de_ur';

module.exports.partner = '0';

const details = {
    name: {
        type: 'text',
        value: 'Abc',
        newValue: 'New Longname'
    },
    longname: 'Abc Abc Abc',
    inn: '111222333',
    phone: {
        type: 'text',
        value: '12345678',
        newValue: '87654321'
    },
    email: 'aa@bb.cc',
    countryId: { type: 'select', value: 'Австралия' },
    city: 'Some city',
    legaladdress: 'street, 500',
    postaddress: 'street, 700',
    postcode: '123456',
    payType: { type: 'select', value: 'Расчетный счет' },
    account: '96284558338824',
    swift: 'SABRRUMM'
};

module.exports.details = processDetails(details);
