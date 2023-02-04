const { processDetails, Types } = require('../helpers');

module.exports.personType = 'yt_kzp';

module.exports.partner = '0';

const details = {
    lname: 'Фамилия',
    fname: 'Имя',
    mname: 'Отчество',
    phone: {
        type: 'text',
        value: '+7 727 123-45-78',
        newValue: '+7 727 875-43-21'
    },
    fax: '+7 727 999 11 22',
    email: 'dart-vaider@yandex.kz',
    countryId: { type: 'select', value: 'Казахстан' },
    postcode: '349323',
    city: 'Алматы',
    postaddress: 'ул. Назарбаева, д. 1',
    invalidAddress: {
        type: Types.checkbox,
        value: true
    },
    kzIn: '012345678912',
    verifiedDocs: {
        type: Types.checkbox,
        value: true
    },
    file: { type: 'file', value: 'testfile.docx' }
};

module.exports.details = processDetails(details);
