module.exports.personType = 'il_ur';
module.exports.partner = '1';

const details = {
    name: {
        type: 'text',
        value: 'Israeli legal partner Payer'
    },
    longname: {
        type: 'text',
        value: 'longname',
        newValue: 'New Longname Israeli legal partner Payer'
    },
    inn: {
        type: 'text',
        value: '9934728933042'
    },
    ilId: {
        type: 'text',
        value: '333776429'
    },
    localName: {
        type: 'text',
        value: 'כותרת'
    },
    phone: {
        type: 'text',
        value: '+9 72 000000000'
    },
    fax: {
        type: 'text',
        value: '+ 972 30044 2933'
    },
    email: {
        type: 'text',
        value: 'email@email.ru'
    },
    countryId: {
        type: 'select',
        value: 'Израиль'
    },
    city: {
        type: 'text',
        value: 'Some city'
    },
    representative: {
        type: 'text',
        value: 'Vasily'
    },
    localRepresentative: {
        type: 'text',
        value: 'יעקב'
    },
    postaddress: {
        type: 'text',
        value: 'Israel Tel aviv Test 33'
    },
    localPostaddress: {
        type: 'text',
        value: 'כתובת דואר'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    legaladdress: {
        type: 'text',
        value: 'street, 500'
    },
    localLegaladdress: {
        type: 'text',
        value: 'כתובת משפטית'
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
        value: '123451234512345'
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
