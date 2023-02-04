const { processDetails, Types } = require('../helpers');

module.exports.personType = 'de_ph';

module.exports.partner = '0';

const details = {
    lname: 'Lastname',
    fname: 'Firstname',
    phone: {
        type: 'text',
        value: '+7 495 123-45-67',
        newValue: '+7 727 434 22 33'
    },
    fax: '+7 333 123-45-67',
    email: '789@0123.567',
    countryId: { type: Types.select, value: 'Германия' },
    postcode: '888222',
    city: 'Berlin',
    postaddress: 'Main street',
    file: { type: Types.file, value: 'testfile.docx' },
    verifiedDocs: {
        type: Types.checkbox,
        value: true
    },
    note: 'asd',
    purchaseOrder: 'PO-123456'
};

module.exports.details = processDetails(details);
