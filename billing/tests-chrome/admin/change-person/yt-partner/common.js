const { processDetails, Types } = require('../helpers');

module.exports.personType = 'yt';

module.exports.partner = '1';

const details = {
    name: { type: Types.text, value: 'Old Company Name', newValue: 'New Company Name' },
    phone: '+7 123 456 7890',
    fax: '+7 000 456 7890',
    email: 'asdfasdfadslfkjhdaslfk@sldkfsdlfkjdlsfkj.ru',
    representative: 'Billy',
    postcode: '555000',
    address: 'Somewhere over the rainbow',
    countryId: { type: Types.select, value: 'Аруба' },
    invalidAddress: { type: Types.checkbox },
    invalidBankprops: { type: Types.checkbox },
    longname: 'Old Company Full Name',
    legalAddressPostcode: '555xxx',
    legaladdress: 'Yes, it is',
    bank: 'Babank',
    bankcity: 'Someburg',
    benAccount: '000 mIEMb',
    swift: 'SABRRUMM',
    corrSwift: 'SABRRUMM',
    inn: '3358359869',
    payType: { type: Types.select, value: 'IBAN' },
    iban: 'LF2Hro8EPmcnGk2rVCmiDd',
    deliveryCity: { type: Types.select, value: 'Новосибирск' },
    signerPersonName: 'Harry',
    signerPositionName: { type: Types.select, value: 'General Director' },
    authorityDocType: { type: Types.select, value: 'Приказ' },
    authorityDocDetails: 'No, it is not'
};

module.exports.details = processDetails(details);
