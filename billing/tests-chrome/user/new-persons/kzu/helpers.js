const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Тест корп.');
    await browser.setValue(elements.phone, '88006004433');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'ГОродок');
    await browser.setValue(elements.postaddress, 'Адрес селениум 5');
    await browser.setValue(elements.legaladdress, 'Юр. адрес');
    await browser.setValue(elements.kz_in, '123456789012');
    await browser.setValue(elements.kbe, '12');
    await browser.setValue(elements.bik, 'ABCDEFGH');
    await browser.setValue(elements.iik, '12345678901234567890');
};

module.exports.fillAllFields = async function (browser) {
    await browser.setValue(elements.name, 'Тест корп.');
    await browser.setValue(elements.phone, '88006004433');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.city, 'ГОродок');
    await browser.setValue(elements.postaddress, 'Адрес селениум 5');
    await browser.setValue(elements.legaladdress, 'Юр. адрес');
    await browser.setValue(elements.kz_in, '123456789012');
    await browser.setValue(elements.kbe, '12');
    await browser.setValue(elements.bik, 'ABCDEFGH');
    await browser.setValue(elements.iik, '12345678901234567890');

    await browser.setValue(elements.longname, 'Полное название');
    await browser.setValue(elements.fax, '12345678');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    await browser.setValue(elements.rnn, '123456789012');
    await browser.setValue(elements.bank, 'Ya Bank');
    await browser.setValue(elements.corr_swift, 'ADCTJPJJ');
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');
    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page);
    await browser.scroll(elements.city);
    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page);
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.longname);
    await browser.setValue(elements.longname, 'Тест');

    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '91231233232');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '123123123');

    await browser.ybClearValue(elements.postcode);
    await browser.setValue(elements.postcode, '123456');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'город');

    await browser.ybClearValue(elements.postaddress);
    await browser.setValue(elements.postaddress, 'почтовый адрес');

    await browser.ybClearValue(elements.kbe);
    await browser.setValue(elements.kbe, '12');

    await browser.ybClearValue(elements.bik);
    await browser.setValue(elements.bik, 'ABCDEFGH');

    await browser.ybClearValue(elements.bank);
    await browser.setValue(elements.bank, 'Ya bank');

    await browser.ybClearValue(elements.corr_swift);
    await browser.setValue(elements.corr_swift, 'ADCTJPJJ');

    await browser.ybClearValue(elements.iik);
    await browser.setValue(elements.iik, '12345678900987654321');
};
