const { elements } = require('../../../../../admin/change-person/elements');

module.exports.fillRequiredFieldsForPhDetails = async function (browser, details) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.mname, details.mname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.click('div[data-detail-id="agree"] input');
};

module.exports.fillAllFieldsPhUser = async function (browser, details) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.mname, details.mname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    await browser.click('span=выберите страну');
    await browser.click('span=Россия');
    await browser.setValue(elements.fax, details.fax.value);
    await browser.setValue(elements.postcode, details.postcode.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.setValue(elements.bik, details.bik.value);
    await browser.setValue(elements.account, details.account.value);
    await browser.setValue(elements.corraccount, details.corraccount.value);
    await browser.setValue(elements.bank, details.bank.value);
    await browser.setValue(elements.bankcity, details.bankcity.value);

    await browser.click('div[data-detail-id="agree"] input');
};
