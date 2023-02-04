module.exports.personType = 'eu_yt';

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
        newValue: 'Zyx'
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
        value: '12345678'
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
        value: 'street, 500'
    },
    localPostaddress: {
        type: 'text',
        value: 'street, 500 Local'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    countryId: {
        type: 'select',
        value: 'Беларусь'
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
    }
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = details;
