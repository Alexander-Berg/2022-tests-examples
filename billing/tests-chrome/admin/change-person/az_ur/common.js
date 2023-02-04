const { processDetails, Types } = require('../helpers');

module.exports.personType = 'az_ur';

module.exports.partner = '0';

const details = {
    name: {
        value: 'org name',
        isMandatory: true
    },
    longname: 'long org name',
    inn: {
        value: '123456789',
        isMandatory: true
    },
    localName: 'local name',
    phone: {
        value: '+123 11 22 33 44',
        newValue: '+123 99 88 77 66',
        isMandatory: true
    },
    fax: '+123 55 66 77 88',
    email: 'email@mail.mail',
    countryId: { type: Types.select, value: 'Азербайджан' },
    city: 'город',
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
    legaladdress: {
        value: 'legal ad ress',
        isMandatory: true
    },
    localLegaladdress: 'local legal addr',
    invalidAddress: {
        type: Types.checkbox,
        value: true
    },
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    benBankCode: {
        value: '123456',
        isMandatory: true
    },
    payType: { type: Types.select, value: 'Расчетный счет', isMandatory: true },
    account: { value: 'accccount', isMandatory: true },
    swift: { value: 'SABRRUMM', isMandatory: true },
    corrSwift: 'SABRRUMM'
};

module.exports.details = Object.values(processDetails(details));
