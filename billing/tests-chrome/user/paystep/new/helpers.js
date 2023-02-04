const path = require('path');
const { fillRequiredFieldsForUSADetails } = require('./helpers');
const { data } = require('../../../admin/change-person/ur/data');
const { elements } = require('../../../admin/change-person/elements');

module.exports.fillRequiredFieldsForUr = async function (browser) {
    await browser.setValue(elements.name, 'рогачев');
    await browser.waitForVisible(`.Suggest-Item*=РОГАЧЕВЪ`);
    await browser.click(`.Suggest-Item*=РОГАЧЕВЪ`);
    await browser.ybClearValue(elements.ogrn);
    await browser.setValue(elements.phone, data.add.ur.phone);
    await browser.setValue(elements.email, 'hello@hello.com');
    await browser.ybClearValue(elements.legalAddressStreet);
    await browser.setValue(elements.legalAddressPostcode, '123456');
    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
    await browser.setValue(elements.postbox, data.add.ur.postbox);
};

module.exports.submitAndWaitPopUp = async function (browser, elements) {
    await browser.click(elements.mainButtons.submit);
    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
    await browser.waitForVisible(elements.popup);
};

module.exports.fillRequiredFieldsForPh = async function (browser) {
    await browser.setValue(elements.lname, 'иванов');
    await browser.setValue(elements.fname, 'иван');
    await browser.setValue(elements.mname, 'иванович');
    await browser.setValue(elements.phone, '+7 800 555-35-35');
    await browser.setValue(elements.fax, '+7 800 555-35-39');
    await browser.setValue(elements.email, 'hello@hello.com');
    //country
    await browser.setValue(elements.postcode, '111033');
    await browser.setValue(elements.city, 'Москва');
    await browser.setValue(elements.postaddress, 'ул. Строителей, д. 123, корп. 4, кв. 567');
    await browser.click('div[data-detail-id="agree"] input');
    await browser.setValue(elements.bik, '044525225');
    await browser.setValue(elements.corraccount, 'yandexMoney');
    await browser.setValue(elements.bank, 'sberbank');
    await browser.setValue(elements.bankcity, 'ул. 8 Марта');
};

module.exports.waitNavigationToInvoice = async function (browser) {
    await browser.waitUntil(
        async function () {
            const url = await browser.getUrl();
            return url.includes('/invoice.xml?invoice_id=');
        },
        { timeout: 30000 }
    );
};

module.exports.waitUntilTimeout = 100000;

module.exports.setCurrency = async function (browser, details) {
    await browser.ybSetSteps(`Устанавливаем ${details.currency.value} как валюту`);
    await browser.click('.yb-paystep-main__currency');
    await browser.click(`${details.currency.id}`);
    await browser.ybWaitForLoad();
};

module.exports.setPaymethod = async function (browser, payMethod) {
    await browser.click('.yb-paystep-main__pay-method button');
    switch (payMethod) {
        case 'bank':
            await browser.click('div[id="bank"]');
            break;
        case 'card':
            await browser.click('div[id="card"]');
            break;
        case 'yamoney':
            await browser.click('div[id="yamoney_wallet"]');
            break;
        case 'webmoney':
            await browser.click('div[id="webmoney_wallet"]');
            break;
        default:
            return 'непонятный способ оплаты';
            break;
    }
    await browser.ybWaitForLoad();
};

module.exports.hideElements = ['.yb-user-copyright__year'];

module.exports.waitUntilTimeout = 5000;
