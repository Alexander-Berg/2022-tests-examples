const { elements } = require('../../../../../admin/change-person/elements');
const path = require('path');

module.exports.fillRequiredFieldsForSW_PhDetails = async function (
    browser,
    details,
    role = 'client',
    type = 'sw_ph'
) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    if (role === 'client') {
        const selector = `div input[type=file]`;
        const filePath = path.join(__dirname, 'testfile.docx');
        const remotePath = await browser.uploadFile(filePath);
        const elem = await browser.$(selector);
        await elem.addValue(remotePath);
    } else {
        await browser.click('input[name="verified-docs"]');
    }

    if (type === 'sw_ytph') {
        await browser.click('span=выберите страну');
        await browser.click(`span=${details.country.name}`);
    }
};

module.exports.fillAllFieldsForeignUser = async function (browser, details) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);
    await browser.setValue(elements.fax, details.fax.value);
    await browser.setValue(elements.postcode, details.postcode.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.setValue(elements.purchaseOrder, details.purchaseOrder.value);

    const selector = `div input[type=file]`;
    const filePath = path.join(__dirname, 'testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(selector);
    await elem.addValue(remotePath);
};
