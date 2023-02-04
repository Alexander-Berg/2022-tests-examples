const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Y. && Y.');
    await browser.setValue(elements.longname, 'Long name');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.postaddress, 'Greetings my beloved Yandex Billing');
    await browser.setValue(elements.legaladdressInput, 'Konstantin street 15');
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.name, 'Y. && Y.');
    await browser.setValue(elements.longname, 'Long name');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.postaddress, 'Greetings my beloved Yandex Billing');
    await browser.setValue(elements.legaladdressInput, 'Konstantin street 15');

    await browser.setValue(elements.vat_number, 'UKXX123456789');
    await browser.setValue(elements.phone, '987654321');
    await browser.setValue(elements.account, '9999999999');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}`, elements.page);
    // await browser.scroll(elements.postaddress);
    // await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.vat_number);
    await browser.setValue(elements.vat_number, 'UKXX123456789');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.account);
    await browser.setValue(elements.account, '123123123');

    await browser.ybClearValue(elements.longname);
    await browser.setValue(elements.longname, 'Long name');
};
