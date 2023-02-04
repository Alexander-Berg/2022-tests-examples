const { processDetails, Types } = require('../helpers');

module.exports.personType = 'hk_yt';

module.exports.partner = '1';

const details = {
    name: { value: 'hk company', isMandatory: true },
    longname: { value: 'hk full company', isMandatory: true },
    phone: { value: '+123 33 45 66 44', newValue: '+123 99 88 77 66', isMandatory: true },
    fax: '+123 11 22 33 44',
    email: { value: 'hk_email@yandex.ru', isMandatory: true },
    representative: 'hk representative',
    signerPersonName: 'person name',
    signerPositionName: { type: 'select', value: 'Президент' },
    authorityDocType: { type: 'select', value: 'Свидетельство о регистрации' },
    city: { value: 'Гонконг', isMandatory: true },
    postaddress: { value: 'адрес в гонконге', isMandatory: true },
    postcode: { value: '12345HK', isMandatory: true },
    countryId: { type: 'select', value: 'Китай', isMandatory: true, scrollIntoView: true },
    legaladdress: {
        type: Types.textarea,
        value: 'юрюрюрюрюрюрюр\nадададад\nресссссссс д 1'
    },
    inn: '1234567',
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    payType: { type: Types.select, value: 'Расчетный счет', isMandatory: true },
    account: { value: 'accccount', isMandatory: true },
    swift: { value: 'SABRRUMM', isMandatory: true },
    corrSwift: 'SABRRUMM',
    paymentPurpose: { value: 'Очень деньги нужны', isMandatory: true, isAdmin: true }
};

module.exports.details = Object.values(processDetails(details));
