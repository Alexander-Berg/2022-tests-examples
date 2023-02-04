const { elements, ignoreElements } = require('../elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Test Corp');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.phone, '88005003020');
    await browser.setValue(elements.email, 'test@test.test');

    await browser.setValue(elements.postaddress, 'City, 1, home');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.legaladdressInput, 'test street 5, city');

    await browser.setValue(elements.ben_bank_code, '123456');

    await browser.click(elements.payType.button);
    await browser.click(elements.payType.other.list);
    await browser.setValue(elements.swift, 'ADCTJPJJ');
    await browser.setValue(elements.payType.other.input, 'others');
};

module.exports.fillAllFields = async function (browser, payType = '') {
    await browser.setValue(elements.name, 'Test Corp');
    await browser.setValue(elements.inn, '123456789');
    await browser.setValue(elements.phone, '88005003020');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.postaddress, 'City, 1, home');
    await browser.setValue(elements.postcode, '123123');
    await browser.setValue(elements.legaladdressInput, 'test street 5, city');
    await browser.setValue(elements.ben_bank_code, '123456');

    await browser.setValue(elements.longname, 'full name');
    await browser.setValue(elements.fax, '123456789');

    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');

    await browser.setValue(elements.city, 'CIty');
    await browser.setValue(elements.representative, 'Contact Face');

    if (payType) {
        await browser.click('[data-detail-id="payType"]');
        switch (payType) {
            case 'IBAN':
                await browser.click(elements.payType.iban.list);
                await browser.setValue(elements.payType.iban.input, '12345678');
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
                await browser.setValue(elements.ben_bank, 'imya poluchatelya');
                await browser.setValue(elements.corr_swift, 'ABSRNOK1XXX');
                break;
            case 'Расчетный счет':
                await browser.click(elements.payType.account.list);
                await browser.setValue(elements.payType.account.input, '12345678');
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
                await browser.setValue(elements.corr_swift, 'ABSRNOK1XXX');
                break;
            case 'Прочее':
                await browser.click(elements.payType.other.list);
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
                await browser.setValue(elements.corr_swift, 'ABSRNOK1XXX');
                await browser.setValue(elements.payType.other.input, 'others..');
                break;
            default:
                break;
        }
    }
};

module.exports.ReplaceAllPossibleFields = async function (browser) {
    await browser.ybClearValue(elements.phone);
    await browser.setValue(elements.phone, '111111');

    await browser.ybClearValue(elements.fax);
    await browser.setValue(elements.fax, '222222');

    await browser.ybClearValue(elements.email);
    await browser.setValue(elements.email, 'test@test.test');

    await browser.ybClearValue(elements.representative);
    await browser.setValue(elements.representative, 'Contact A.A.');

    await browser.ybClearValue(elements.city);
    await browser.setValue(elements.city, 'city');

    await browser.ybClearValue(elements.ben_bank_code);
    await browser.setValue(elements.ben_bank_code, '123456');

    await browser.click('[data-detail-id="payType"]');
    await browser.click(elements.payType.other.list);
    await browser.setValue(elements.swift, 'ABSRNOK1XXX');
    await browser.setValue(elements.corr_swift, 'ABSRNOK1XXX');
    await browser.setValue(elements.payType.other.input, 'others..');
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
