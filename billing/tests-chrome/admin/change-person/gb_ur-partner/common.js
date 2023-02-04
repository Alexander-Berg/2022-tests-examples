const { processDetails, Types } = require('../helpers');

module.exports.personType = 'gb_ur';

module.exports.partner = '1';

const details = {
    name: {
        type: 'text',
        value: 'Abc',
        newValue: 'Zyx'
    },
    longname: {
        type: 'text',
        value: 'Abc Abc Abc'
    },
    vatNumber: {
        type: 'text',
        value: 'UKXX999999999'
    },
    inn: {
        type: 'text',
        value: '123123123'
    },
    localName: {
        type: 'text',
        value: 'Local Abc Abc Abc'
    },
    phone: {
        type: 'text',
        value: '12345678'
    },
    fax: {
        type: 'text',
        value: '12121212'
    },
    email: 'asdfda@adsfadsfads.aba',
    countryId: {
        type: 'select',
        value: 'Великобритания'
    },
    city: {
        type: 'text',
        value: 'London'
    },
    representative: {
        type: 'text',
        value: 'qwerty'
    },
    localRepresentative: {
        type: 'text',
        value: 'qwerty1'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    postaddress: {
        type: 'text',
        value: 'street, 500'
    },
    localPostaddress: {
        type: 'text',
        value: 'local street, 500'
    },
    legaladdress: {
        type: 'text',
        value: 'local street, 400'
    },
    localLegaladdress: {
        type: 'text',
        value: 'local street, 400'
    },
    invalidAddress: {
        type: 'checkbox'
    },
    account: {
        type: 'text',
        value: '12345'
    },
    invalidBankprops: {
        type: 'checkbox'
    },
    payType: {
        type: 'select',
        value: 'IBAN'
    },
    iban: {
        type: 'text',
        value: '12345678'
    },
    swiftOpt: {
        type: 'text',
        value: 'ABSRNOK1XXX'
    },
    corrSwiftOpt: {
        type: 'text',
        value: 'ABSRNOK1XXX'
    },
    benBank: {
        type: 'text',
        value: '12345678'
    },
    localBenBank: {
        type: 'text',
        value: '12345678'
    }
};

module.exports.details = processDetails(details);
