const details = {
    lname: { value: 'Müller', newValue: 'Schmid' },
    fname: { value: 'Maria', newValue: 'Christian' },
    phone: { value: '031-775 04 41', newValue: '0346-521 52' },
    email: { value: 'test@address.test', newValue: 'diablo@belialh.leg' },
    fax: { value: '+70000000000', newValue: '+71111111111' },
    country: { name: 'Швейцария', countryId: '225' },
    postcode: { value: '587 31', newValue: '621 34' },
    file: { type: 'file', value: 'testfile.docx' },
    city: { value: 'Geneva', newValue: 'Weu' },
    purchaseOrder: { value: 'PO-123123', newValue: 'PO 09876543211' },
    postaddress: {
        value: 'Linkoping, Soderleden 22, South Central',
        newValue: 'Organistvagen 5, Falkenberg, South-East'
    },
    currency: { id: '#CHF', value: 'швейцарский франк' }
};

const personType = { name: 'sw_ytph', value: 'Физ. лицо-нерезидент, Швейцария' };

module.exports = {
    personType,
    details
};
