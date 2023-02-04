const { processDetails, Types } = require('../helpers');

module.exports.personType = 'us_yt';

module.exports.partner = '0';

const details = {
    name: {
        value: 'Abc',
        newValue: 'Zyx',
        isMandatory: true
    },
    longname: 'Abc Abc Abc',
    phone: {
        value: '12345678',
        isMandatory: true
    },
    fax: {
        value: '12121212'
    },
    representative: {
        value: 'Vasily'
    },
    signerPersonName: {
        value: 'Inokenty'
    },
    signerPositionName: {
        type: Types.select,
        value: 'Президент'
    },
    purchaseOrder: 'PO-123456',
    city: { value: 'Some city', isMandatory: true },
    authorityDocType: {
        type: Types.select,
        value: 'Устав'
    },
    postaddress: {
        value: 'street, 500',
        isMandatory: true
    },
    postcode: {
        value: '123456',
        isMandatory: true
    },
    countryId: {
        type: Types.select,
        value: 'США',
        scrollIntoView: true,
        isMandatory: true
    },
    legaladdress: 'street, 500',
    inn: '111222333',
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    payType: {
        type: Types.select,
        value: 'Расчетный счет'
    },
    account: '96284558338824',
    swift: 'SABRRUMM',
    corrSwift: 'SABRRUMM',
    file: {
        type: 'file',
        value: 'testfile.docx',
        isAdmin: false,
        isMandatory: true
    },
    verifiedDocs: {
        type: 'checkbox',
        value: true,
        isAdmin: true
    }
};

module.exports.details = Object.values(processDetails(details));
