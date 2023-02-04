const { processDetails, Types } = require('../helpers');

module.exports.personType = 'us_ytph';

module.exports.partner = '0';

const details = {
    fname: { value: 'USA nonres fname', isMandatory: true },
    lname: { value: 'USA nonres lname', isMandatory: true },
    phone: {
        value: '+12345678',
        newValue: '+17654321',
        isMandatory: true
    },
    fax: '+12121212',
    city: 'Some city',
    purchaseOrder: 'PO-123456',
    postaddress: 'street, 500',
    postcode: '123456',
    countryId: { type: 'select', value: 'США', isMandatory: true },
    file: {
        type: 'file',
        value: 'testfile.docx',
        isAdmin: false,
        isMandatory: true
    },
    verifiedDocs: {
        type: Types.checkbox,
        value: true,
        newValue: false,
        isAdmin: true
    }
};

module.exports.details = Object.values(processDetails(details));
