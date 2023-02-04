const { elements, ignoreElements } = require('../elements');

module.exports.takeScreenshots = async function (browser, text) {
    await browser.scroll('h1');

    await browser.ybAssertView(`new-persons, ${text}, часть 1`, elements.page, { ignoreElements });

    await browser.scroll('[data-detail-id="passportD"]');

    await browser.ybAssertView(`new-persons, ${text}, часть 2`, elements.page, { ignoreElements });

    await browser.scroll('[data-detail-id="legalAddrType"]');

    await browser.ybAssertView(`new-persons, ${text}, часть 3`, elements.page, { ignoreElements });
};

module.exports.replaceValues = async function (browser) {
    await browser.ybClearValue('[name="lname"]');
    await browser.setValue('[name="lname"]', 'Тестов');

    await browser.ybClearValue('[name="fname"]');
    await browser.setValue('[name="fname"]', 'Тест');

    await browser.ybClearValue('[name="mname"]');
    await browser.setValue('[name="mname"]', 'Тестович');

    await browser.ybClearValue('[data-detail-id="birthday"] input');
    await browser.setValue('[data-detail-id="birthday"] input', '16.03.2022');

    await browser.ybClearValue('[name="passport-birthplace"]');
    await browser.setValue('[name="passport-birthplace"]', 'Мой дом');

    await browser.ybClearValue('[name="birthplace-district"]');
    await browser.setValue('[name="birthplace-district"]', 'Район');

    await browser.ybClearValue('[name="birthplace-region"]');
    await browser.setValue('[name="birthplace-region"]', 'Моя область');

    await browser.ybClearValue('[name="birthplace-country"]');
    await browser.setValue('[name="birthplace-country"]', 'ОЗ');

    await browser.ybClearValue('[name="passport-s"]');
    await browser.setValue('[name="passport-s"]', '7777');

    await browser.ybClearValue('[name="passport-n"]');
    await browser.setValue('[name="passport-n"]', '123456');

    await browser.ybClearValue('[data-detail-id="passportD"] input');
    await browser.setValue('[data-detail-id="passportD"] input', '16.03.2022 г.');

    await browser.ybClearValue('[name="passport-e"]');
    await browser.setValue('[name="passport-e"]', 'отделом по выдачи документов');

    await browser.ybClearValue('[name="passport-code"]');
    await browser.setValue('[name="passport-code"]', '123-456');

    await browser.ybClearValue('[name="phone"]');
    await browser.setValue('[name="phone"]', '81231231212');

    await browser.ybClearValue('[name="email"]');
    await browser.setValue('[name="email"]', 'test@test.test');

    await browser.click('div[data-detail-id="countryId"] button');
    await browser.click('span=Абхазия');

    await browser.click('div[data-detail-id="deliveryType"] button');
    await browser.click('span=VIP');

    await browser.click('div[data-detail-id="deliveryCity"] button');
    await browser.click('span=Казань');

    await browser.ybClearValue('[name="legaladdress"]');
    await browser.setValue('[name="legaladdress"]', 'Приезжай сюда!!');

    await browser.ybClearValue('[name="bik"]');
    await browser.setValue('[name="bik"]', '044030653');

    await browser.ybClearValue('[name="pfr"]');
    await browser.setValue('[name="pfr"]', '12312312312');

    await browser.click('[data-detail-id="bankType"] button');
    await browser.click('span=другой банк');

    await browser.setValue('[name="person-account"]', '123123');

    await browser.ybClearValue('[name="bank-inn"]');
    await browser.setValue('[name="bank-inn"]', '7712345678');

    await browser.ybClearValue('[name="payment-purpose"]');
    await browser.setValue('[name="payment-purpose"]', 'я хочу питсы');

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Москва');
    await browser.ybWaitForInvisible('.suggest__spin-container');
    await browser.click('mark=Москва');

    await browser.ybClearValue('[name="postcode"]');
    await browser.setValue('[name="postcode"]', '123456');

    await browser.ybClearValue('[name="postsuffix"]');
    await browser.setValue('[name="postsuffix"]', 'а/я 123');

    await browser.ybClearValue('[name="account"]');
    await browser.setValue('[name="account"]', '40817810455000000131');
};
