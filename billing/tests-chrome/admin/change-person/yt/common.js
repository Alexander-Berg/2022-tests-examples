const { processDetails, Types } = require('../helpers');

module.exports.personType = 'yt';

module.exports.partner = '0';

const details = {
    name: { value: 'R & K', isMandatory: true, newValue: 'New name' },
    countryId: { type: Types.select, value: 'Бурунди', isMandatory: true },
    phone: { value: '+1234567890', isMandatory: true },
    fax: '+0987654321',
    representative: 'Batman',
    postcode: '123456',
    address: { value: 'Road near the forest', isMandatory: true },
    invalidAddress: { type: Types.checkbox, value: true },
    invalidBankprops: { type: Types.checkbox, value: true },
    longname: { value: 'Rogas and Kopytas', isMandatory: true },
    legalAddressPostcode: '123456',
    legaladdress: {
        type: Types.textarea,
        value: 'Some street, some building, 155',
        isMandatory: true
    },
    bank: 'Babank',
    account: '123abc321',
    deliveryType: { type: Types.select, value: 'почта' },
    deliveryCity: { type: Types.select, value: 'Москва' },
    liveSignature: { type: Types.checkbox, value: true },
    signerPersonName: 'Mr Smith',
    signerPersonGender: { type: Types.select, value: 'мужской' },
    signerPositionName: { type: Types.select, value: 'Президент' },
    authorityDocType: { type: Types.select, value: 'Приказ' },
    authorityDocDetails: 'Detali tali tali tali',
    vip: { type: Types.checkbox, value: true },
    earlyDocs: { type: Types.checkbox, value: true }
};

module.exports.details = Object.values(processDetails(details));
