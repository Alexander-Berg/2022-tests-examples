const { processDetails } = require('../helpers');

const data = {
    add: {
        ur: {
            inn: '1114',
            name: 'А/К 1114',
            phone: '+1234567890',
            fax: '+0987654321',
            representative: 'Баба Нюра',
            reviseActPeriodType: 'ежемесячно',
            postcodeSimple: '123123',
            postbox: 'а/я 123',
            bik: '044030001',
            account: '40702810500000000000',
            address: 'Москва, ул. Такая, д. 500',
            deliveryType: 'почта',
            deliveryCity: 'Казань',
            signerPersonName: 'Васисуалий Лоханкин',
            signerPersonGender: 'мужской',
            signerPositionName: 'Президент',
            authorityDocDetails: 'Детали нам не дали',
            authorityDocType: 'Приказ',
            kbk: '18210101090012200110',
            oktmo: '12345678',
            paymentPurpose: 'Просто деньги очень нужны',
            legaladdress: 'Там, где-то там',
            ...processDetails({
                city: { type: 'suggest', suggestValue: 'вышний вол', value: 'Вышний' }
            })
        }
    }
};

module.exports.data = data;
