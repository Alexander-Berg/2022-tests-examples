const { processDetails, Types } = require('../helpers');

module.exports.personType = 'byu';

module.exports.partner = '1';

const details = {
    name: 'LTD, Belarus',
    phone: '+41 32 5617074',
    fax: '+41 32 6298409',
    email: 'Mj^q@vfOx.YFB',
    representative: 'asdf',
    countryId: { type: 'select', value: 'Беларусь' },
    postcode: '810700',
    city: 'city Kdt',
    postaddress: 'Street 4',
    invalidAddress: { type: Types.checkbox },
    invalidBankprops: { type: Types.checkbox },
    inn: '272242202',
    longname: 'Long LTD BELARUS',
    legaladdress: 'Avenue 4',
    benBank: 'Bank ydOZ',
    swift: 'SABRRUMM',
    account: 'BY56ALFA56751218795427663075',
    deliveryType: { type: Types.select, value: 'почта' },
    deliveryCity: { type: Types.select, value: 'Казань' },
    liveSignature: { type: Types.checkbox },
    signerPersonName: 'фывафывафыва',
    signerPersonGender: { type: 'select', value: 'мужской' },
    signerPositionName: { type: 'select', value: 'Президент' },
    authorityDocType: { type: 'select', value: 'Приказ' },
    authorityDocDetails: 'Деньги очень нужны',
    vip: { type: Types.checkbox },
    earlyDocs: { type: Types.checkbox }
};

module.exports.details = processDetails(details);
