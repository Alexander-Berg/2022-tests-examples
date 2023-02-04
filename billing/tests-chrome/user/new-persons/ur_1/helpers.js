const { elements, ignoreElements } = require('../elements');

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');

    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page, { ignoreElements });

    await browser.scroll('[data-detail-id="representative"]');

    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page, { ignoreElements });

    await browser.scroll('[data-detail-id="invalidBankprops"]');

    await browser.ybAssertView(`new-persons, ${text}, часть 3`, elements.page, { ignoreElements });
};

module.exports.replaceValues = async function (browser) {
    await browser.ybClearValue('[name="name"]');
    await browser.setValue('[name="name"]', 'ООО Компания');
    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.ybClearValue('[name="longname"]');
    await browser.setValue('[name="longname"]', 'Мы не Кампания а Компания');

    await browser.ybClearValue('[name="kpp"]');
    await browser.setValue('[name="kpp"]', '305443717');

    await browser.ybClearValue('[name="ogrn"]');
    await browser.setValue('[name="ogrn"]', '1234567890123');

    await browser.ybClearValue('[name="phone"]');
    await browser.setValue('[name="phone"]', '81231231212');

    await browser.ybClearValue('[name="email"]');
    await browser.setValue('[name="email"]', 'test@test.test');

    await browser.ybClearValue('[name="fax"]');
    await browser.setValue('[name="fax"]', '81231231212');

    await browser.click('div[data-detail-id="countryId"] button');
    await browser.click('span=Абхазия');

    await browser.ybClearValue('[name="representative"]');
    await browser.setValue('[name="representative"]', 'КОнтактное лицо');

    await browser.ybClearValue('[name="legaladdress"]');
    await browser.setValue('[name="legaladdress"]', 'адрес ахах 2');

    await browser.ybClearValue('[name="postcode"]');
    await browser.setValue('[name="postcode"]', '123456');

    await browser.ybClearValue('[name="postsuffix"]');
    await browser.setValue('[name="postsuffix"]', 'а/я 123');

    await browser.ybClearValue('[name="bik"]');
    await browser.setValue('[name="bik"]', '044030653');

    await browser.ybClearValue('[name="account"]');
    await browser.setValue('[name="account"]', '40817810455000000131');

    await browser.click('div[data-detail-id="deliveryType"] button');
    await browser.click('span=курьер Яндекса');

    await browser.click('div[data-detail-id="deliveryCity"] button');
    await browser.click('span=Казань');

    await browser.ybClearValue('[name="signer-person-name"]');
    await browser.setValue('[name="signer-person-name"]', 'Я подписал А.А.');

    await browser.click('[data-detail-id="signerPersonGender"]');
    await browser.click('span=мужской');

    await browser.click('[data-detail-id="signerPositionName"] button');
    await browser.click('span=Генеральный директор');

    await browser.click('[data-detail-id="authorityDocType"] button');
    await browser.click('span=Устав');

    await browser.ybClearValue('[name="authority-doc-details"]');
    await browser.setValue('[name="authority-doc-details"]', 'основание и всякие детали');

    await browser.ybClearValue('[name="kbk"]');
    await browser.setValue('[name="kbk"]', '12345678901234567890');

    await browser.ybClearValue('[name="oktmo"]');
    await browser.setValue('[name="oktmo"]', '12345678');

    await browser.ybClearValue('[name="payment-purpose"]');
    await browser.setValue('[name="payment-purpose"]', 'я хочу питсы');

    await browser.click('[data-detail-id="invalidBankprops"] input');
    await browser.click('[data-detail-id="invalidAddress"] input');
};
