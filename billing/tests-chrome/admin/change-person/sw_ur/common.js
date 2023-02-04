const { processDetails, Types } = require('../helpers');

module.exports.personType = 'sw_ur';

module.exports.partner = '0';

const details = {
    name: {
        type: 'text',
        value: 'Abc',
        newValue: 'New Longname'
    },
    longname: 'Abc Abc Abc',
    phone: {
        type: 'text',
        value: '12345678',
        newValue: '87654321'
    },
    fax: '12121212',
    representative: 'Vasily',
    signerPersonName: 'Inokenty',
    signerPositionName: { type: 'select', value: 'Президент' },
    city: 'Some city',
    postaddress: 'street, 500',
    postcode: '123456',
    countryId: { type: 'select', value: 'Австралия' },
    legaladdress: 'street, 500',
    inn: '111222333',
    invalidBankprops: { type: Types.checkbox },
    payType: { type: 'select', value: 'Расчетный счет' },
    account: '96284558338824',
    swift: 'SABRRUMM',
    file: { type: 'file', value: 'testfile.docx' },
    verifiedDocs: { type: Types.checkbox },
    purchaseOrder: 'PO-123456'
};

module.exports.details = processDetails(details);
