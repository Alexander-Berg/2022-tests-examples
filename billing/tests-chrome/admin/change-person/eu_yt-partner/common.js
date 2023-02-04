const { processDetails, Types } = require('../helpers');

module.exports.personType = 'eu_yt';

module.exports.partner = '1';

const details = {
    name: 'EU nonres legal Payer',
    longname: 'Long EU nonres legal Payer',
    localName: 'Local EU nonres legal Payer',
    phone: '+41 32 2304106',
    fax: '+41 32 8617302',
    email: 'Mj^q@vfOx.YFB',
    representative: 'Нерезидент, TaxiBVBcz',
    localRepresentative: 'plaatselijke vertegenwoordiger',
    postaddress: 'Улица 5',
    localPostaddress: 'lokaal postadres',
    postcode: '123456',
    countryId: { type: 'select', value: 'Беларусь' },
    legaladdress: 'Avenue 5',
    invalidAddress: { type: Types.checkbox },
    inn: '658500388',
    invalidBankprops: { type: Types.checkbox },
    payType: { type: 'select', value: 'Расчетный счет' },
    account: '96284558338824',
    iban: 'NL91ABNA0417164300',
    swift: 'SABRRUMM',
    swiftOpt: 'SABRRUMM',
    corrSwift: 'SABRRUMM',
    corrSwiftOpt: 'SABRRUMM',
    benBank: 'Ben Bank',
    localBenBank: 'naam lokale Bank'
};

module.exports.details = processDetails(details);
