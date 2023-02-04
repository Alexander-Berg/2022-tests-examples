const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ur';

module.exports.partner = '1';

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
    lname: { type: Types.text, value: 'Стараяфамилия', newValue: 'Новаяфамилия' },
    fname: { type: Types.text, value: 'Староеимя', newValue: 'Новоеимя' },
    mname: { type: Types.text, value: 'Староеотчество', newValue: 'Новоеотчество' },
    longname: 'кока кола',
    kpp: '912788793',
    ogrn: '1023601070140',
    email: 'Mj^q@vfOx.YFB',
    phone: { type: Types.text, value: '+7 812 3017123', newValue: '+7 000 1112233' },
    fax: '+7 812 1121638',
    countryId: { type: 'select', value: 'Россия' },
    representative: 'SWpsT',
    legalAddrType: { type: 'radio', value: '1', newValue: '2' },
    legaladdress: 'Та еще улица',
    legalAddressCity: { type: 'suggest', suggestValue: 'химк', value: 'Химки' },
    legalAddressStreet: { type: 'suggest', suggestValue: 'липовая', value: 'Липовая' },
    legalAddressPostcode: '555555',
    legalAddressHome: '5к5стр500',
    invalidAddress: { type: 'checkbox' },
    isPostbox: { type: 'radio', value: '0', newValue: '1' },
    isSamePostaddress: { type: 'checkbox' },
    city: { type: 'suggest', suggestValue: 'вышний вол', value: 'Вышний' },
    street: { type: 'suggest', suggestValue: 'айва', value: 'Айвазовского' },
    postcode: '123456',
    postsuffix: '1000',
    postcodeSimple: '654321',
    postbox: '1000/500',
    invalidBankprops: { type: 'checkbox' },
    bik: '044525440',
    account: '40702810982208554168',
    bankType: { type: 'select', value: 'СБП (Система Быстрых Платежей)' },
    fpsPhone: '79999999999',
    fpsBank: { type: 'select', value: 'ВТБ' },
    deliveryType: { type: 'select', value: 'почта' },
    deliveryCity: { type: 'select', value: 'Казань' },
    signerPersonName: 'фывафывафыва',
    signerPersonGender: { type: 'select', value: 'мужской' },
    signerPositionName: { type: 'select', value: 'Президент' },
    authorityDocType: { type: 'select', value: 'Приказ' },
    authorityDocDetails: 'Деньги очень нужны',
    kbk: '18210202140061110160',
    oktmo: '45301000',
    paymentPurpose: '123'
};

module.exports.details = processDetails(details);
