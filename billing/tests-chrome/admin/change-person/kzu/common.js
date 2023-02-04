const { processDetails, Types } = require('../helpers');

module.exports.personType = 'kzu';

module.exports.partner = '0';

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
        value: 'Казахстан'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    city: {
        type: 'text',
        value: 'Нур-Султан'
    },
    localCity: {
        type: 'text',
        value: 'Nur-Sultan'
    },
    postaddress: {
        type: 'text',
        value: 'street, 500'
    },
    localPostaddress: {
        type: 'text',
        value: 'local street, 500'
    },
    invalidBankprops: {
        type: 'checkbox'
    },
    legaladdress: {
        type: 'text',
        value: 'street, 500'
    },
    localLegaladdress: {
        type: 'text',
        value: 'local street, 500'
    },
    rnn: {
        type: 'text',
        value: '301306050855'
    },
    kzIn: {
        type: 'text',
        value: '496227421585'
    },
    kbe: {
        type: 'text',
        value: '18'
    },
    bik: {
        type: 'text',
        value: 'CASPKZKA'
    },
    bank: {
        type: 'text',
        value: 'Банк omi'
    },
    corrSwift: {
        type: 'text',
        value: 'SABRRUMM'
    },
    localBank: {
        type: 'text',
        value: 'Local банк'
    },
    iik: {
        type: 'text',
        value: 'KZ838560000000463517'
    },
    signerPersonName: {
        type: 'text',
        value: 'Барбамбулык'
    },
    localSignerPersonName: {
        type: 'text',
        value: 'Локальный Барбамбулык'
    },
    signerPersonGender: {
        type: 'select',
        value: 'мужской'
    },
    signerPositionName: {
        type: 'select',
        value: 'Президент'
    },
    localSignerPositionName: {
        type: 'text',
        value: 'Локальный Президент'
    },
    authorityDocType: {
        type: 'select',
        value: 'Приказ'
    },
    authorityDocDetails: {
        type: 'text',
        value: 'Деньги очень нужны'
    },
    localAuthorityDocDetails: {
        type: 'text',
        value: 'Локальный приказ'
    }
};

module.exports.details = processDetails(details);
