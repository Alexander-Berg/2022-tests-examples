const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ytph';

module.exports.partner = '0';

const details = {
    fname: 'Вейдер',
    lname: 'Дарт',
    phone: {
        type: 'text',
        value: '8 800 555 35 55',
        newValue: '+7 495 495 55 55'
    },
    fax: '8 800 888 88 88',
    email: 'dart@vaider.yandex',
    countryId: { type: 'select', value: 'Мадагаскар' },
    postcode: '666777',
    city: 'Екатеринбург',
    postaddress: 'ул. Пушкина, д. Колотушкина',
    agree: {
        type: Types.checkbox,
        value: true
    },
    verifiedDocs: {
        type: Types.checkbox,
        value: true
    },
    file: { type: 'file', value: 'testfile.docx' }
};

module.exports.details = processDetails(details);
