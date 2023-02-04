const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ur_ytkz';

module.exports.partner = '0';

const details = {
    ownershipTypeUi: {
        type: 'select',
        value: 'Организация',
        newValue: 'Индивидуальный предприниматель'
    },
    name: {
        type: 'text',
        value: 'Xyz',
        newValue: 'Zyx',
        isMandatory: true
    },
    longname: {
        type: 'text',
        value: 'Xyz Xyz Xyz'
    },
    localName: {
        type: 'text',
        value: 'Xyz Xyz Xyz Local'
    },
    phone: {
        type: 'text',
        value: '12345678',
        isMandatory: true
    },
    fax: {
        type: 'text',
        value: '12121212'
    },
    representative: {
        type: 'text',
        value: 'Vasily'
    },
    localRepresentative: {
        type: 'text',
        value: 'Vasily Local'
    },
    postaddress: {
        type: 'text',
        value: 'street, 500',
        isMandatory: true
    },
    localPostaddress: {
        type: 'text',
        value: 'street, 500 Local'
    },
    postcode: {
        type: 'text',
        value: '123456',
        isMandatory: true
    },
    countryId: {
        type: 'select',
        value: 'Беларусь',
        isMandatory: true
    },
    invalidAddress: {
        type: 'checkbox'
    },
    inn: {
        type: 'text',
        value: '111222333'
    },
    legaladdress: {
        type: 'text',
        value: 'street, 500, city'
    },
    invalidBankprops: {
        type: 'checkbox'
    },
    payType: {
        type: 'select',
        value: 'Расчетный счет',
        isMandatory: true
    },
    account: {
        type: 'text',
        value: '96284558338824',
        isMandatory: true
    },
    swift: {
        type: 'text',
        value: 'SABRRUMM',
        isMandatory: true
    },
    corrSwift: {
        type: 'text',
        value: 'SABRRUMM'
    }
};

module.exports.details = Object.values(processDetails(details));
