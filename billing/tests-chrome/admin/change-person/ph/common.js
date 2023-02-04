const { processDetails, Types } = require('../helpers');

module.exports.personType = 'ph';

module.exports.partner = '0';

const details = {
    lname: { value: 'Башмачкин', isMandatory: true, newValue: 'Башмачкин' },
    fname: { value: 'Акакий', isMandatory: true, newValue: 'Акакий' },
    mname: { value: 'Акакиевич', isMandatory: true, newValue: 'Акакиевич' },
    phone: { value: '+78881112233', isMandatory: true, newValue: '+78881112233' },
    fax: '+70000000000',
    countryId: { type: Types.select, value: 'Аруба' },
    postcode: '123456',
    city: 'Мытищи',
    postaddress: 'Улица Трудоголиков, д. 100, кв. 500',
    invalidAddress: { type: Types.checkbox, value: true },
    invalidBankprops: { type: Types.checkbox, value: true },
    bik: '044030001',
    account: '40702810500000000000',
    corraccount: '30101810400000000225',
    bank: 'Дыня и Тыква',
    bankcity: 'Новый-Город',
    paymentPurpose: 'Деньги нужны',
    agree: { type: Types.checkbox, value: true, isMandatory: true }
};

module.exports.details = Object.values(processDetails(details));
