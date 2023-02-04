const { elements, ignoreElements } = require('../elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.fname, 'Person');
    await browser.setValue(elements.lname, 'Lname');
    await browser.setValue(elements.phone, '91231233232');
    await browser.setValue(elements.email, 'test@test.test');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.fname, 'Person');
    await browser.setValue(elements.lname, 'Lname');
    await browser.setValue(elements.phone, '91231233232');
    await browser.setValue(elements.email, 'test@test.test');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);

    await browser.setValue(elements.fax, '123123123');
    await browser.setValue(elements.purchase_order, '123');
    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
    await browser.setValue(elements.city, 'City');
    await browser.setValue(elements.postcode, '1234');
    await browser.setValue(elements.postaddress, 'CIty 123, street 5');
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.purchase_order);
    await browser.setValue(elements.purchase_order, '123');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');

    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page, {
        ignoreElements
    });

    await browser.scroll(elements.postcode);

    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page, {
        ignoreElements
    });
};
