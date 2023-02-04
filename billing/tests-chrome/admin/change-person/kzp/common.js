const { processDetails, Types } = require('../helpers');

module.exports.personType = 'kzp';
module.exports.partner = '0';

const details = {
    lname: 'Тестерова',
    fname: 'Анна',
    mname: 'Тестеровна',
    phone: '+41 32 5617074',
    fax: '+41 32 6298409',
    email: 'Mj^q@vfOx.YFB',
    countryId: { type: 'select', value: 'Казахстан' },
    postcode: '810700',
    city: 'city Kdt',
    postaddress: 'Street 4',
    kzIn: '123456789012'
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = processDetails(details);
