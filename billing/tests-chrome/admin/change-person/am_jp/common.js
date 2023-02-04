const { processDetails, Types } = require('../helpers');

module.exports.personType = 'am_jp';

module.exports.partner = '0';

const details = {
    name: { value: 'Название организации', isMandatory: true },
    localName: 'Локальное название',
    longname: { value: 'Полное название', isMandatory: true },
    localLongname: 'Локальное полное название',
    legaladdress: {
        type: Types.textarea,
        value: 'юрюрюрюрюрюрюр\nадададад\nресссссссс д 1',
        isMandatory: true
    },
    localLegaladdress: {
        type: Types.textarea,
        value: 'Локальный юрюрюрюрюрюрюр\nадададад\nресссссссс д 1'
    },
    inn: { value: '88888888', isMandatory: true },
    rn: '123123',
    jpc: 'AM100110101010111',
    phone: {
        value: '+374 10 10 10 10',
        newValue: '+374 99 99 99 99',
        isMandatory: true
    },
    fax: '+374 11 22 33 44',
    email: { value: 'am_jp@yandex.ru', isMandatory: true },
    representative: 'Контактное Лицо',
    localRepresentative: 'Локальное контактное лицо',
    countryId: { type: 'select', value: 'Армения' },
    postcode: { value: '1234', isMandatory: true },
    city: 'Ереван',
    localCity: 'Ереван Ереван',
    postaddress: { value: 'адрес адрес адрес', isMandatory: true },
    localPostaddress: 'адрес адрес адрес',
    invalidAddress: { type: Types.checkbox, value: true },
    invalidBankprops: { type: Types.checkbox, value: true },
    account: { value: '12345', isMandatory: true },
    benBank: { value: 'BABANK', isMandatory: true },
    localBenBank: 'BABABANK',
    swift: { value: 'SABRRUMM', isMandatory: true }
};

module.exports.details = Object.values(processDetails(details));
