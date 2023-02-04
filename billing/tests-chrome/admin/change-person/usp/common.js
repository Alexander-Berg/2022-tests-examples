const { processDetails, Types } = require('../helpers');

module.exports.personType = 'usp';
module.exports.partner = '0';

const details = {
    lname: 'Smith',
    fname: 'Alex',
    phone: '+41 32 5617074',
    email: 'Mj^q@vfOx.YFB',
    purchaseOrder: 'PO-123456',
    countryId: { type: 'select', value: 'США' },
    postcode: '810700',
    city: 'city Kdt',
    postaddress: 'Street 4',
    usState: { type: 'select', value: 'Colorado' },
    verifiedDocs: { type: 'checkbox' }
};

Object.keys(details).forEach(key => (details[key].id = key));

module.exports.details = processDetails(details);
