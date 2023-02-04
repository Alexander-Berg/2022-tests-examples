const { processDetails, Types } = require('../helpers');

module.exports.personType = 'de_ytph';

module.exports.partner = '0';

const details = {
    fname: 'German nonres fname',
    lname: 'German nonres lname',
    phone: {
        type: 'text',
        value: '12345678',
        newValue: '87654321'
    },
    fax: '1717171717',
    email: 'aa@bb.cc',
    city: 'Some city',
    postaddress: 'street, 500',
    postcode: '123456',
    countryId: { type: 'select', value: 'Австралия' },
    file: { type: 'file', value: 'testfile.docx' },
    verifiedDocs: {
        type: Types.checkbox,
        value: true,
        newValue: false
    }
};

module.exports.details = processDetails(details);
