const { elements, ignoreElements } = require('../elements');
const path = require('path');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue(elements.name, 'Test Corp');
    await browser.setValue(elements.phone, '88005003020');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.city, 'City');
    await browser.setValue(elements.postaddress, 'City, 1, home');
    await browser.setValue(elements.postcode, '123123');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);
};

module.exports.fillAllFields = async function (browser, payType = '') {
    await browser.setValue(elements.name, 'Test Corp');
    await browser.setValue(elements.phone, '88005003020');
    await browser.setValue(elements.email, 'test@test.test');
    await browser.setValue(elements.city, 'City');
    await browser.setValue(elements.postaddress, 'City, 1, home');
    await browser.setValue(elements.postcode, '123123');

    const filePath = path.join(__dirname, '../testfile.docx');
    const remotePath = await browser.uploadFile(filePath);
    const elem = await browser.$(elements.file);
    await elem.addValue(remotePath);

    await browser.setValue(elements.longname, 'Full Test Corp Name');
    await browser.setValue(elements.fax, '4543454');
    await browser.click(elements.countryIdButton);
    await browser.click('span=Абхазия');
    await browser.setValue(elements.representative, 'Contact Face');
    await browser.setValue(elements.signer_person_name, 'Person M.A.');
    await browser.click(elements.signerPositionNameButton);
    await browser.click('span=Директор');
    await browser.setValue(elements.purchase_order, '123');
    await browser.setValue(elements.legaladdress, '123 city, home, street 5/2');
    await browser.setValue(elements.inn, '87654321');

    if (payType) {
        await browser.click('[data-detail-id="payType"]');
        switch (payType) {
            case 'IBAN':
                await browser.click(elements.payType.iban.list);
                await browser.setValue(elements.payType.iban.input, '12345678');
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
                await browser.setValue(elements.ben_bank, 'imya poluchatelya');
                break;
            case 'Расчетный счет':
                await browser.click(elements.payType.account.list);
                await browser.setValue(elements.payType.account.input, '12345678');
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
                break;
            case 'Прочее':
                await browser.click(elements.payType.other.list);
                await browser.setValue(elements.swift, 'ABSRNOK1XXX');
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

    await browser.ybClearValue(elements.signer_person_name);
    await browser.setValue(elements.signer_person_name, 'FIO f f');

    await browser.click(elements.signerPositionNameButton);
    await browser.click('span=Президент');

    await browser.ybClearValue(elements.purchase_order);
    await browser.setValue(elements.purchase_order, '123');

    await browser.click('[data-detail-id="payType"]');
    await browser.click(elements.payType.other.list);
    await browser.setValue(elements.swift, 'ABSRNOK1XXX');
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
