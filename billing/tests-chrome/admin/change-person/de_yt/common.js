const { processDetails, Types } = require('../helpers');

module.exports.personType = 'de_yt';

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
    phone: {
        type: 'text',
        value: '12345678'
    },
    email: 'aa@bb.cc',
    signerPersonName: {
        type: 'text',
        value: 'Inokenty'
    },
    signerPositionName: {
        type: 'select',
        value: 'Президент'
    },
    city: {
        type: 'text',
        value: 'Some city'
    },
    authorityDocType: {
        type: 'select',
        value: 'Устав'
    },
    postaddress: {
        type: 'text',
        value: 'street, 500'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    countryId: {
        type: 'select',
        value: 'Беларусь',
        scrollIntoView: true
    },
    legaladdress: {
        type: 'text',
        value: 'street, 500'
    },
    inn: {
        type: 'text',
        value: '111222333'
    },
    invalidBankprops: {
        type: 'checkbox'
    },
    payType: {
        type: 'select',
        value: 'Расчетный счет'
    },
    account: {
        type: 'text',
        value: '96284558338824'
    },
    swift: {
        type: 'text',
        value: 'SABRRUMM'
    },
    corrSwift: {
        type: 'text',
        value: 'SABRRUMM'
    },
    verifiedDocs: {
        type: 'checkbox'
    },
    purchaseOrder: {
        type: 'text',
        value: 'PO-123456'
    }
};

module.exports.details = processDetails(details);
