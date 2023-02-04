const { processDetails, Types } = require('../helpers');

module.exports.personType = 'usu';
module.exports.partner = '0';

const details = {
    name: 'Test Org',
    phone: '+41 32 5617074',
    email: 'Mj^q@vfOx.YFB',
    representative: 'representative',
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
