const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ph_autoru';

module.exports.partner = '0';

const details = {
    lname: 'Фамилия',
    fname: 'Имя',
    mname: 'Отчество',
    phone: { type: Types.text, value: '123123', newValue: '000000' },
    fax: '234234',
    email: 'asd@asd.asd',
    countryId: { type: 'select', value: 'Россия' },
    postcode: '123456',
    city: 'Москва',
    postaddress: 'Почтовый адрес',
    invalidAddress: { type: 'checkbox' },
    agree: { type: 'checkbox' }
};

module.exports.details = processDetails(details);
