const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.lname, 'Тестов');
    await browser.setValue(elements.fname, 'Тест');
    await browser.setValue(elements.mname, 'Тестович');
    await browser.setValue(elements.organization, 'Органзиация по написанию тестов');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'Город');
    await browser.setValue(elements.postaddress, 'Улица тестов 5');
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.lname, 'Тестов');
    await browser.setValue(elements.fname, 'Тест');
    await browser.setValue(elements.mname, 'Тестович');
    await browser.setValue(elements.organization, 'Органзиация по написанию тестов');
    await browser.setValue(elements.phone, '+712312312323');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'Город');
    await browser.setValue(elements.postaddress, 'Улица тестов 5');

    await browser.setValue(elements.fax, '8123123123');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.fname);
    await browser.setValue(elements.fname, 'Тест');

    await browser.ybClearValue(elements.lname);
    await browser.setValue(elements.lname, 'Тестов');

    await browser.ybClearValue(elements.mname);
    await browser.setValue(elements.mname, 'Тестович');

    await browser.ybClearValue(elements.organization);
    await browser.setValue(elements.organization, 'Очень серьезные люди');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '123123123');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'Город');

    await browser.ybClearValue(elements.postcode);
    await browser.setValue(elements.postcode, '1234');

    await browser.ybClearValue(elements.postaddress);
    await browser.setValue(elements.postaddress, 'Автоматизированные тесты, улица селениума д 7');
};
