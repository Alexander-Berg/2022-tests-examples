const { processDetails, Types } = require('../helpers');

module.exports.personType = 'yt_kzu';

module.exports.partner = '0';

const details = {
    name: 'Название компании',
    longname: 'ООО "Название компании"',
    phone: {
        type: 'text',
        value: '+7 727 123 45 67',
        newValue: '+7 727 434 22 33'
    },
    fax: '+7 727 999 11 22',
    email: 'e@ma.il',
    countryId: { type: Types.select, value: 'Казахстан' },
    postcode: '888222',
    city: 'Нурсултан',
    postaddress: 'ул. Назарбаева, д. 1',
    invalidAddress: {
        type: Types.checkbox,
        value: true
    },
    invalidBankprops: {
        type: Types.checkbox,
        value: true
    },
    legaladdress: { type: Types.textarea, value: 'юр\nад\nрес' },
    rnn: '123456789012',
    kzIn: '123456789012',
    file: { type: Types.file, value: 'testfile.docx' },
    bik: 'KZBICBIC',
    bank: 'Банк Казах',
    iik: 'KZ111222333444555666',
    verifiedDocs: {
        type: Types.checkbox,
        value: true
    },
    deliveryType: { type: Types.select, value: 'почта' },
    signerPersonName: 'Фамилия Имя Отчество',
    signerPersonGender: { type: Types.select, value: 'мужской' },
    signerPositionName: { type: Types.select, value: 'Управляющий' },
    authorityDocType: { type: Types.select, value: 'Устав' },
    authorityDocDetails: 'Деньги очень нужны',
    vip: {
        type: Types.checkbox,
        value: true
    }
};

module.exports.details = processDetails(details);
