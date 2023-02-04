const { processDetails, Types } = require('../helpers');

module.exports.personType = 'kzu';

module.exports.partner = '1';

const details = {
    name: 'KZ legal Payer',
    longname: 'local full name',
    localName: 'Жергілікті атауы',
    phone: '+7 727 1732223',
    fax: '+7 727 7534114',
    email: 'usp@email.kz',
    countryId: { type: Types.select, value: 'Казахстан' },
    postcode: '9841a',
    city: 'py_city',
    localCity: 'жергілікті қала',
    postaddress: 'Py_Street 5',
    localPostaddress: 'жергілікті пошта мекенжайы',
    invalidBankprops: { type: Types.checkbox },
    legaladdress: 'Avenue 3',
    localLegaladdress: 'жергілікті заңды мекен-жайы',
    rnn: '301306050855',
    kzIn: '496227421585',
    kbe: '18',
    bik: 'CASPKZKA',
    bank: 'Банк omi',
    corrSwift: 'SABRRUMM',
    localBank: 'банктің жергілікті атауы',
    iik: 'KZ838560000000463517',
    signerPersonName: 'repr',
    localSignerPersonName: 'жергілікті өкіл',
    signerPersonGender: { type: Types.select, value: 'женский' },
    signerPositionName: { type: Types.select, value: 'Управляющий' },
    localSignerPositionName: 'жергілікті лауазымы',
    authorityDocType: { type: Types.select, value: 'Приказ' },
    authorityDocDetails: 'cerHL',
    localAuthorityDocDetails: 'жергілікті бөлшектер'
};

module.exports.details = processDetails(details);
