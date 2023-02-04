const details = {
    name: {
        type: 'text',
        value: 'ООО Бульба',
        newValue: 'ООО Цыбуля'
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
        value: 'Василий'
    },
    countryId: {
        type: 'select',
        value: 'Беларусь'
    },
    postcode: {
        type: 'text',
        value: '123456'
    },
    city: {
        type: 'text',
        value: 'Бобруйск'
    },
    postaddress: {
        type: 'text',
        value: 'где эта улица, 15'
    },
    invalidAddress: {
        type: 'checkbox'
    },
    invalidBankprops: {
        type: 'checkbox'
    },
    inn: {
        type: 'text',
        value: '111222333'
    },
    longname: {
        type: 'text',
        value: 'Бульба Бульба Бульба'
    },
    legaladdress: {
        type: 'text',
        value: 'Бобруйск Бобруйск Бобруйск'
    },
    benBank: {
        type: 'text',
        value: '55555benbank'
    },
    swift: {
        type: 'text',
        value: 'SABRRUMM'
    },
    account: {
        type: 'text',
        value: '12345account'
    },
    deliveryType: {
        type: 'select',
        value: 'почта'
    },
    deliveryCity: {
        type: 'select',
        value: 'Москва'
    },
    liveSignature: {
        type: 'checkbox'
    },
    signerPersonName: {
        type: 'text',
        value: 'Иванов Иван Иванович'
    },
    signerPersonGender: {
        type: 'select',
        value: 'мужской'
    },
    signerPositionName: {
        type: 'select',
        value: 'Президент'
    },
    authorityDocType: {
        type: 'select',
        value: 'Приказ'
    },
    authorityDocDetails: {
        type: 'text',
        value: 'Деньги очень нужны'
    },
    vip: {
        type: 'checkbox'
    },
    earlyDocs: {
        type: 'checkbox'
    }
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = details;
