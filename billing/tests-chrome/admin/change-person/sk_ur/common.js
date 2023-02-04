const { processDetails, Types } = require('../helpers');

module.exports.personType = 'sk_ur';

module.exports.partner = '0';

module.exports.isUr = true;

const details = {
    name: { value: 'Org name', isMandatory: true },
    longname: { value: 'Org full name', isMandatory: true },
    inn: '123-12-12345',
    countryId: { type: 'select', value: 'Южная Корея' },
    city: {
        value: 'Сеул',
        isMandatory: true
    },
    legaladdress: { value: 'legal address' },
    postaddress: { value: 'post address', isMandatory: true },
    postcode: { value: '123456', isMandatory: true },
    phone: {
        value: '+82-02-312-3456',
        newValue: '+82-02-312-0000',
        isMandatory: true
    },
    email: {
        value: 'email@yandex.ru',
        isMandatory: true,
        newValue: 'yandex@email.ru'
    },
    benBank: {
        value: 'BABANK',
        newValue: 'BABABANK'
    },
    account: {
        value: '96284558338824',
        newValue: '12344558338824'
    },
    swift: {
        value: 'SABRRUMM',
        newValue: 'ALFARUMM'
    }
};

module.exports.details = Object.values(processDetails(details));
