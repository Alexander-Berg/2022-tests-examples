const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ro_ur';

module.exports.partner = '0';

const details = {
    name: {
        value: 'org name',
        isMandatory: true
    },
    longname: 'long org name',
    inn: '123456789',
    localName: 'local name',
    phone: {
        value: '+123 11 22 33 44',
        newValue: '+123 99 88 77 66',
        isMandatory: true
    },
    fax: '+123 55 66 77 88',
    email: { value: 'email@mail.mail', isMandatory: true },
    countryId: { type: Types.select, value: 'Румыния', isMandatory: true, scrollIntoView: true },
    representative: 'контактное лицо',
    localRepresentative: 'локальное контактное лицо',
    postaddress: {
        value: 'адрес',
        isMandatory: true
    },
    localPostaddress: 'локальный адрес',
    postcode: {
        value: '1234AZ',
        isMandatory: true
    },
    invalidAddress: {
        type: Types.checkbox,
        value: true
    },
    legaladdress: {
        value: 'legal address'
    },
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    payType: { type: Types.select, value: 'Расчетный счет', isMandatory: true },
    account: { value: 'accccount', isMandatory: true },
    swift: { value: 'SABRRUMM', isMandatory: true },
    corrSwift: 'SABRRUMM'
};

module.exports.details = Object.values(processDetails(details));
