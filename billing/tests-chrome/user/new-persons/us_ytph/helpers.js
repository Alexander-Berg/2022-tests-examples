const { elements, ignoreElements } = require('../elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.lname, 'Testov');
    await browser.setValue(elements.fname, 'Test');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.lname, 'Testov');
    await browser.setValue(elements.fname, 'Test');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    await browser.setValue(elements.fax, '123123123');

    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'city');
    await browser.setValue(elements.postaddress, 'test st 5');
    await browser.setValue(elements.purchase_order, '123');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page);
    await browser.scroll(elements.city);
    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.fname);
    await browser.setValue(elements.fname, 'FIrstTest');

    await browser.ybClearValue(elements.lname);
    await browser.setValue(elements.lname, 'Testov');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.purchase_order);
    await browser.setValue(elements.purchase_order, '123123');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '5454545454');

    await browser.ybClearValue(elements.postcode);
    await browser.setValue(elements.postcode, '123456');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'Vsem Privet');

    await browser.ybClearValue(elements.postaddress);
    await browser.setValue(elements.postaddress, 'Menya zovut');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};
