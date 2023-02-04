const { elements } = require('../../../../../admin/change-person/elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser, details, role) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);

    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.city, details.city.value);

    await browser.setValue(elements.postaddress, details.postaddress.value);
    await browser.click('div[data-detail-id="agree"] input');
    if (role === 'user') {
        const selector = `div input[type=file]`;
        console.log(__dirname + '/testfile.docx');
        const filePath = path.join(__dirname, '/testfile.docx');
        const remotePath = await browser.uploadFile(filePath);
        const elem = await browser.$(selector);
        await elem.addValue(remotePath);
    }
};

module.exports.fillAllFields = async function (browser, details) {
    await browser.setValue(elements.lname, details.lname.value);
    await browser.setValue(elements.fname, details.fname.value);

    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.city, details.city.value);

    await browser.setValue(elements.postaddress, details.postaddress.value);

    await browser.click('div[data-detail-id="agree"] input');

    const selector = `div input[type=file]`;
    const filePath = path.join(__dirname, '/testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(selector);
    await elem.addValue(remotePath);
};
