const { elements } = require('../../../../../admin/change-person/elements');

module.exports.fillRequiredFieldsForKZHDetails = async function (browser, details, role) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.setValue(elements.postcode, details.postcode.value);
    await browser.setValue('input[name="kz-in"]', details.kz_in.value);
    await browser.setValue(elements.postaddress, details.postaddress.value);
};

module.exports.fillAllFieldsForKZHDetails = async function (browser, details) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.mname, details.mname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.setValue(elements.postcode, details.postcode.value);
    await browser.setValue('input[name="kz-in"]', details.kz_in.value);
    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.setValue(elements.fax, details.fax.value);
    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);
};

module.exports.hideElements = ['.yb-user-copyright__year'];
