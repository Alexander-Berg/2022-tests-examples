const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.lname, 'Фамилия');
    await browser.setValue(elements.fname, 'Имя');
    await browser.setValue(elements.phone, '123456789');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123456');
    await browser.setValue(elements.city, 'Городище');
    await browser.setValue(elements.postaddress, 'Город 1, тест 5');
    await browser.setValue(elements.kz_in, '123456789012');
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.lname, 'Фамилия');
    await browser.setValue(elements.fname, 'Имя');
    await browser.setValue(elements.phone, '123456789');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123456');
    await browser.setValue(elements.city, 'Городище');
    await browser.setValue(elements.postaddress, 'Город 1, тест 5');
    await browser.setValue(elements.kz_in, '123456789012');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
    await browser.setValue(elements.mname, 'Тестович');
    await browser.setValue(elements.fax, '1234567890');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.lname);
    await browser.setValue(elements.lname, 'Тестов');

    await browser.ybClearValue(elements.fname);
    await browser.setValue(elements.fname, 'Тест');

    await browser.ybClearValue(elements.mname);
    await browser.setValue(elements.mname, 'Тестович');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '123456789');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '987654321');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.postcode);
    await browser.setValue(elements.postcode, '123456');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'Городочек');

    await browser.ybClearValue(elements.postaddress);
    await browser.setValue(elements.postaddress, 'Городочек, улица Seleniuma, 2');
};
