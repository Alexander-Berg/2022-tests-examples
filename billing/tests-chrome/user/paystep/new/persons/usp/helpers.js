const { elements } = require('../../../../../admin/change-person/elements');
const path = require('path');
module.exports.fillRequiredFieldsForUSADetails = async function (browser, details, role) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    if (role === 'user') {
        const selector = `div input[type=file]`;
        const filePath = path.join(__dirname, 'testfile.docx');
        const remotePath = await browser.uploadFile(filePath);
        const elem = await browser.$(selector);
        await elem.addValue(remotePath);
    } else {
        await browser.click('input[name="verified-docs"]');
    }

    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.click('span=не выбрано');
    await browser.click(`span=${details.state.value}`);
    await browser.setValue(elements.postcode, details.postcode.value);
};

module.exports.fillAllFieldsUSA = async function (browser, details, role) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.purchaseOrder, details.purchaseOrder.value);

    if (role === 'user') {
        const selector = `div input[type=file]`;
        const filePath = path.join(__dirname, 'testfile.docx');
        const remotePath = await browser.uploadFile(filePath);
        const elem = await browser.$(selector);
        await elem.addValue(remotePath);
    } else {
        await browser.click('input[name="verified-docs"]');
    }

    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.setValue(elements.city, details.city.value);
    await browser.click('span=не выбрано');
    await browser.click(`span=${details.state.value}`);
    await browser.setValue(elements.postcode, details.postcode.value);

    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);
};
