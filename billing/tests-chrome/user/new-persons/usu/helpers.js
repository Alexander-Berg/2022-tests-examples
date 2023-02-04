const { elements, ignoreElements } = require('../elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Org name');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'City');

    await browser.setValue(elements.postaddress, 'st testov 5');
    await browser.click(elements.usStateButton);
    await browser.click('span=Alabama');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.name, 'Org name');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'City');

    await browser.setValue(elements.postaddress, 'st testov 5');
    await browser.click(elements.usStateButton);
    await browser.click('span=Alabama');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);

    await browser.setValue(elements.representative, 'hello contact face');

    await browser.setValue(elements.purchase_order, '123');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page);
    await browser.scroll(elements.city);
    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.representative);
    await browser.setValue(elements.representative, 'Contact Face');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.purchase_order);
    await browser.setValue(elements.purchase_order, '123123');
};
