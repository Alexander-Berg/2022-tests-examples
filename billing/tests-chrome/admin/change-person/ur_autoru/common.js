const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ur_autoru';

module.exports.partner = '0';

const details = {
    inn: {
        type: Types.suggest,
        suggestValue: '5008048645',
        value: 'ДОЛАВТОТРЕЙД',
        customSuggest: true
    },
    name: {
        type: Types.suggest,
        suggestValue: 'долавто',
        value: 'ДОЛАВТОТРЕЙД',
        customSuggest: true
    },
    countryId: { type: 'select', value: 'Россия' },
    longname: 'кока кола',
    legaladdress: { type: Types.textarea, value: 'Та еще улица' },
    kpp: '912788793',
    ogrn: '1023601070140',
    phone: { type: Types.text, value: '+7 812 3017123', newValue: '+7 000 1112233' },
    email: 'Mj^q@vfOx.YFB',
    fax: '+7 812 1121638',
    representative: 'SWpsT',
    invalidAddress: { type: 'checkbox' },
    city: { type: 'suggest', suggestValue: 'вышний вол', value: 'Вышний' },
    street: { type: 'suggest', suggestValue: 'айва', value: 'Айвазовского' },
    postcodeSimple: '123456',
    postbox: '1000',
    invalidBankprops: { type: 'checkbox' },
    bik: '044525440',
    account: '40702810982208554168',
    address: 'еще улица',
    deliveryType: { type: 'select', value: 'почта' },
    deliveryCity: { type: 'select', value: 'Казань' },
    liveSignature: { type: 'checkbox' },
    signerPersonName: 'Президент Авто.ру',
    signerPersonGender: { type: 'select', value: 'мужской' },
    signerPositionName: { type: 'select', value: 'Президент' },
    authorityDocType: { type: 'select', value: 'Приказ' },
    authorityDocDetails: 'Деньги очень нужны',
    vip: { type: 'checkbox' }
};

module.exports.details = processDetails(details);
