const { processDetails, Types } = require('../helpers');

module.exports.personType = 'by_ytph';

module.exports.partner = '0';

const details = {
    lname: {
        value: 'Фамилия',
        isMandatory: true
    },
    fname: {
        value: 'Имя',
        isMandatory: true
    },
    phone: {
        value: '+7 727 123 45 67',
        newValue: '+7 727 434 22 33',
        isMandatory: true
    },
    fax: '+7 727 999 11 22',
    email: {
        value: 'e@ma.il',
        isMandatory: true
    },
    countryId: {
        type: Types.select,
        value: 'Беларусь'
    },
    postcode: '888222',
    city: {
        value: 'Минск',
        isMandatory: true
    },
    postaddress: {
        value: 'ул. Восстания, д. 20, кв. 21',
        isMandatory: true
    },
    invalidAddress: {
        type: Types.checkbox,
        value: true
    },
    agree: {
        type: Types.checkbox,
        value: true,
        isMandatory: true
    },
    verifiedDocs: {
        type: Types.checkbox,
        value: true,
        isAdmin: true
    },
    file: {
        type: Types.file,
        value: 'testfile.docx',
        isAdmin: false,
        isMandatory: true
    },
    purchaseOrder: 'PO-123456'
};

module.exports.details = Object.values(processDetails(details));
