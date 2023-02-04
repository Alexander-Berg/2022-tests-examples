const { processDetails, Types } = require('../helpers');

module.exports.personType = 'byp';
module.exports.partner = '0';

const details = {
    lname: 'Тестерова',
    fname: 'Анна',
    mname: 'Тестеровна',
    organization: 'Белорусские товары',
    phone: '+41 32 5617074',
    fax: '+39 32 5617074',
    email: 'Mj^q@vfOx.YFB',
    countryId: { type: 'select', value: 'Беларусь' },
    postcode: '810070',
    city: 'city Kdt',
    postaddress: 'Street 4',
    invalidAddress: { type: 'checkbox' }
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = processDetails(details);
