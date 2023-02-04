const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Тест корп.');
    await browser.setValue(elements.phone, '88006004433');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'ГОродок');
    await browser.setValue(elements.postaddress, 'Адрес селениум 5');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.longname, 'Полное название организации');
    await browser.setValue(elements.legaladdress, 'Юр. адрес');
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.name, 'Тест корп.');
    await browser.setValue(elements.phone, '88006004433');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'ГОродок');
    await browser.setValue(elements.postaddress, 'Адрес селениум 5');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.longname, 'Полное название организации');
    await browser.setValue(elements.legaladdress, 'Юр. адрес');

    await browser.setValue(elements.fax, '+71231232323');
    await browser.setValue(elements.representative, 'Контактное лицо');
    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
    await browser.setValue(elements.ben_bank, 'Банк и его имя');
    await browser.setValue(elements.swift, 'ABSRNOK1XXX');
    await browser.setValue(elements.payType.account.input, '40817810455000000131');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page);
    await browser.scroll(elements.postaddress);
    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.name);
    await browser.setValue(elements.name, 'Тест');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '123123123');

    await browser.ybClearValue(elements.representative);
    await browser.setValue(elements.representative, 'Контаткное лицо');

    await browser.ybClearValue(elements.postcode);
    await browser.setValue(elements.postcode, '123456');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'город');

    await browser.ybClearValue(elements.postaddress);
    await browser.setValue(elements.postaddress, 'почтовый адрес');

    await browser.ybClearValue(elements.legaladdress);
    await browser.setValue(elements.legaladdress, 'юр. адрес');

    await browser.ybClearValue(elements.ben_bank);
    await browser.setValue(elements.ben_bank, 'имя банка');

    await browser.ybClearValue(elements.swift);
    await browser.setValue(elements.swift, 'ADCTJPJJXXX');

    await browser.ybClearValue(elements.payType.account.input);
    await browser.setValue(elements.payType.account.input, '123123');
};
