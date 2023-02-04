const details = {
    lname: { value: 'Иванов', newValue: 'Мороз' },
    fname: { value: 'Иван', newValue: 'Сергей' },
    mname: { value: 'Сергеевич', newValue: 'Иванович' },
    phone: { value: '872752212893', newValue: '87275221689' },
    fax: { value: '872752212893', newValue: '87275221689' },
    email: { value: 'test@address.test', newValue: 'diablo@belialh.leg' },
    country: { name: 'Беларусь' },
    postcode: { value: '58731', newValue: '62124' },
    city: { value: 'Бобруйск', newValue: 'Морозино' },
    file: { type: 'file', value: 'testfile.docx' },
    postaddress: {
        value: 'ул. 50 лет ВЛКСМ д. 8, кв 228',
        newValue: 'ул. Пушкина д. Калатушкина 20'
    },
    currency: { id: '#UZS', value: 'узбекский сум' },
    kz_in: { value: '123456789012', newValue: '012123456789' }
};

const personType = { name: 'by_ytph', value: 'Физ. лицо-нерезидент, СНГ' };

module.exports = {
    personType,
    details
};
