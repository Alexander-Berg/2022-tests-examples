module.exports.personType = 'il_ur';

module.exports.partner = '0';

const details = {
    name: {
        type: 'text',
        value: 'Israeli legal Payer',
        newValue: 'New Israeli legal Payer'
    },
    longname: {
        type: 'text',
        value: 'longname'
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
        value: 'TEST KKK'
    },
    phone: {
        type: 'text',
        value: '+9 72 000000000'
    },
    fax: {
        type: 'text',
        value: '+ 972 30044 2933'
    },
    countryId: {
        type: 'select',
        value: 'Беларусь'
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
        value: 'Yasha'
    },
    postaddress: {
        type: 'text',
        value: 'Israel Tel aviv Test 33'
    },
    localPostaddress: {
        type: 'text',
        value: 'Local Israel Tel aviv Test 33'
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
        value: 'IBAN'
    },
    iban: {
        type: 'text',
        value: 'IL470130620000060161516'
    },
    swiftOpt: {
        type: 'text',
        value: 'SABRRUMM'
    },
    corrSwiftOpt: {
        type: 'text',
        value: 'SABRRUMM'
    },
    benBank: {
        type: 'text',
        value: 'TEST ISRAEL'
    },
    localBenBank: {
        type: 'text',
        value: 'TEST NAME'
    }
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = details;
