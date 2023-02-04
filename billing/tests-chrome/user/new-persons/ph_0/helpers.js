const { personType } = require('../common');
const { elements, ignoreElements } = require('../elements');

module.exports.fillRequiredFields = async function (browser) {
    await browser.setValue('[name="lname"]', 'Тестов');
    await browser.setValue('[name="fname"]', 'Тест');
    await browser.setValue('[name="mname"]', 'Тестович');

    await browser.setValue('[name="phone"]', '81231231212');

    await browser.setValue('[name="email"]', 'test@test.test');

    await browser.click('[name="agree"]');
};

module.exports.fillAllFields = async function (browser, isAdmin = false) {
    await browser.setValue('[name="lname"]', 'Тестов');
    await browser.setValue('[name="fname"]', 'Тест');
    await browser.setValue('[name="mname"]', 'Тестович');

    await browser.setValue('[name="phone"]', '81231231212');

    await browser.setValue('[name="email"]', 'test@test.test');

    await browser.click('[name="agree"]');

    await browser.setValue('[name=fax]', '93213213232');
    await browser.click('div[data-detail-id="countryId"] button');
    await browser.waitForVisible('div[role="menuitem"]');
    await browser.click('span=Абхазия');

    await browser.setValue('[name="postcode"]', '123456');
    await browser.setValue('[name="city"]', 'Городочек');
    await browser.setValue('[name="postaddress"]', 'ул Пушкина, дом 12');

    await browser.setValue('[name="bik"]', '044030653');

    await browser.setValue('[name="account"]', '40817810455000000131');

    await browser.setValue('[name="corraccount"]', '86786587578');

    await browser.setValue('[name="bank"]', 'Самый лучший бааанк');

    await browser.setValue('[name="bankcity"]', 'Лучший город д5');

    if (isAdmin) {
        await browser.click('[name="invalid-address"]');
        await browser.click('[name="invalid-bankprops"]');
        await browser.setValue('[name="payment-purpose"]', 'я хочу питсы');
    }
};

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('.yb-user-header-content');

    await browser.ybAssertView(
        `new-persons, ${personType.ph_0.name} ${text}, часть 1`,
        elements.page
    );

    await browser.scroll('[data-detail-id="postaddress"]');

    await browser.ybAssertView(
        `new-persons, ${personType.ph_0.name} ${text}, часть 2`,
        elements.page
    );
};

module.exports.ReplaceValues = async function (browser, isAdmin = false) {
    await browser.ybClearValue('[name="lname"]');
    await browser.setValue('[name="lname"]', 'Тестов');

    await browser.ybClearValue('[name="fname"]');
    await browser.setValue('[name="fname"]', 'Тест');

    await browser.ybClearValue('[name="mname"]');
    await browser.setValue('[name="mname"]', 'Тестович');

    await browser.ybClearValue('[name="phone"]');
    await browser.setValue('[name="phone"]', '81231231212');

    await browser.ybClearValue('[name="email"]');
    await browser.setValue('[name="email"]', 'test@test.test');

    await browser.ybClearValue('[name="fax"]');
    await browser.setValue('[name=fax]', '93213213232');

    await browser.ybClearValue('[name="postcode"]');
    await browser.setValue('[name="postcode"]', '123456');

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Городочек');

    await browser.ybClearValue('[name="postaddress"]');
    await browser.setValue('[name="postaddress"]', 'ул Пушкина, дом 12');

    await browser.ybClearValue('[name="bik"]');
    await browser.setValue('[name="bik"]', '044030653');

    await browser.ybClearValue('[name="account"]');
    await browser.setValue('[name="account"]', '40817810455000000131');

    await browser.ybClearValue('[name="corraccount"]');
    await browser.setValue('[name="corraccount"]', '86786587578');

    await browser.ybClearValue('[name="bank"]');
    await browser.setValue('[name="bank"]', 'Самый лучший бааанк');

    await browser.ybClearValue('[name="bankcity"]');
    await browser.setValue('[name="bankcity"]', 'Лучший город д5');

    if (isAdmin) {
        await browser.click('div[data-detail-id="countryId"] button');
        await browser.click('span=Абхазия');

        await browser.click('[name="invalid-address"]');
        await browser.click('[name="invalid-bankprops"]');

        await browser.ybClearValue('[name="payment-purpose"]');
        await browser.setValue('[name="payment-purpose"]', 'я хочу питсы');
    }
};
