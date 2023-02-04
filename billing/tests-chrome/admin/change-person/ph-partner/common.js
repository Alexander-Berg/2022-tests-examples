const { processDetails } = require('../helpers');

module.exports.personType = 'ph';

module.exports.partner = '1';

const details = {
    lname: {
        type: 'text',
        value: 'Стараяфамилия',
        newValue: 'Новаяфамилия'
    },
    account: {
        value: '40817810455000000131',
        newValue: ''
    },
    fname: 'Иван',
    mname: 'Иванович',
    birthday: {
        type: 'date',
        value: '07.11.1990 г.'
    },
    passportBirthplace: 'д. Такая',
    birthplaceDistrict: 'где-то Рядом',
    birthplaceRegion: 'Московская',
    birthplaceCountry: 'Советский Союз',
    passportS: '1234',
    passportN: '123456',
    passportD: { type: 'date', value: '05.12.2012 г.' },
    passportE: 'Отделение Такое',
    passportCode: '123-456',
    phone: '+70001112233',
    email: 'xxx@aaa.bbb',
    countryId: { type: 'select', value: 'Россия' },
    invalidAddress: { type: 'checkbox' },
    deliveryType: { type: 'select', value: 'почта' },
    deliveryCity: { type: 'select', value: 'Казань' },
    isPostbox: { type: 'radio', value: '0' },
    postcodeSimple: '654321',
    postbox: '1000/500',
    city: { type: 'suggest', suggestValue: 'вышний вол', value: 'Вышний' },
    street: { type: 'suggest', suggestValue: 'айва', value: 'Айвазовского' },
    postcode: '123456',
    postsuffix: '1000',
    legalAddrType: { type: 'radio', value: '1' },
    legaladdress: 'Та еще улица',
    legalAddressCity: { type: 'suggest', suggestValue: 'химк', value: 'Химки' },
    legalAddressStreet: { type: 'suggest', suggestValue: 'липовая', value: 'Липовая' },
    legalAddressPostcode: '555555',
    legalAddressHome: '5к5стр500',
    invalidBankprops: { type: 'checkbox' },
    inn: '375453119207',
    bik: '044030653',
    pfr: '578-139-802 38',
    account: '40817810455000000131',
    bankType: { type: 'select', value: 'СБП (Система Быстрых Платежей)' },
    fpsPhone: '79999999999',
    fpsBank: { type: 'select', value: 'ВТБ' },
    personAccount: '12345',
    webmoneyWallet: 'What is love?',
    bankInn: '3358359869',
    paymentPurpose: 'Деньги очень нужны'
};

module.exports.details = processDetails(details);
