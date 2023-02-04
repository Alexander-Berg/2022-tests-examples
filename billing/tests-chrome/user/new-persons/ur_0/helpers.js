const { elements, ignoreElements } = require('../elements');
const { personType } = require('../common');

module.exports.fillAllFieldsAutoName = async function (browser, value, isAdmin = false) {
    await browser.setValue('input[name=name]', value);
    let valueSelector = `.yb-search-item__name*=${value}`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);

    await browser.setValue('input[name="phone"]', '12345678');
    await browser.setValue('input[name="email"]', 'test@test.test');
    await browser.setValue('input[name="fax"]', '87654321');

    await browser.click('div[data-detail-id="countryId"] button');
    await browser.waitForVisible('div[role="menuitem"]');
    await browser.click('span=Абхазия');

    await browser.setValue('input[name="representative"]', 'Контактное лицо');

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Москва');
    valueSelector = `div[data-search="г Москва"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);

    await browser.setValue('input[name="postcode"]', '123456');
    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.setValue('input[name="postsuffix"]', 'а/я 123');

    await browser.setValue('input[name="bik"]', '044030653');

    await browser.setValue('input[name="account"]', '40817810455000000131');

    await browser.setValue('input[name="kbk"]', '12345678901234567890');
    await browser.setValue('input[name="oktmo"]', '12345678');

    if (isAdmin) {
        await browser.click('[data-detail-id="reviseActPeriodType"] button');
        await browser.click('span=в конце года');

        await browser.setValue('[name="address"]', 'Город 111222 Адрес');

        await browser.click('[data-detail-id="deliveryType"]');
        await browser.click('span=курьер Яндекса');

        await browser.click('[data-detail-id="liveSignature"] input');

        await browser.setValue('[name="signer-person-name"]', 'Подписчик А.А.');

        await browser.click('[data-detail-id="signerPersonGender"]');
        await browser.click('span=мужской');

        await browser.click('[data-detail-id="signerPositionName"] button');
        await browser.click('span=Генеральный директор');

        await browser.click('[data-detail-id="authorityDocType"] button');
        await browser.click('span=Устав');

        await browser.setValue('[name="authority-doc-details"]', 'детали основания');

        await browser.click('[data-detail-id="vip"] input');

        await browser.setValue('[name="payment-purpose"]', 'чтобы протестировать');

        await browser.click('[data-detail-id="invalidBankprops"] input');

        await browser.click('[data-detail-id="invalidAddress"] input');
    }
};

const testData = {
    inn: '3266162051',
    kpp: '251243441',
    email: 'test@test.test',
    ogrn: '',
    legal_address_postcode: '',
    postcode: '123456',
    bik: '',
    account: ''
};

module.exports.fillRequiredFields = async function (browser, data = testData) {
    if (data.inn) await browser.setValue('input[name=inn]', data.inn);
    else await browser.setValue('input[name=inn]', testData.inn);

    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.setValue('input[name=name]', 'Органзиация');
    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.setValue('input[name=longname]', 'Полное названия орг');

    if (data.kpp) await browser.setValue('input[name=kpp]', data.kpp);
    else await browser.setValue('input[name=kpp]', testData.kpp);

    await browser.setValue('input[name="phone"]', '12345678');

    if (data.email) await browser.setValue('input[name="email"]', data.email);
    else await browser.setValue('input[name="email"]', testData.email);

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Москва');
    let valueSelector = `div[data-search="г Москва"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);

    await browser.setValue('textarea[name="legaladdress"]', 'ул уличная');

    if (data.postcode) {
        await browser.setValue('input[name="postcode"]', data.postcode);
        await browser.ybWaitForInvisible('.suggest__spin-container');
    } else {
        await browser.setValue('input[name="postcode"]', testData.postcode);
        await browser.ybWaitForInvisible('.suggest__spin-container');
    }

    await browser.setValue('input[name="postsuffix"]', 'а/я 123');

    if (data.ogrn) await browser.setValue('input[name="ogrn"]', data.ogrn);

    if (data.legal_address_postcode)
        await browser.setValue('input[name="legal-address-postcode"]', data.legal_address_postcode);

    if (data.bik) await browser.setValue('input[name="bik"]', data.bik);

    if (data.account) await browser.setValue('input[name="account"]', data.account);
};

// роли client и admin
module.exports.takeScreenshots = async function (browser, personType, text, role) {
    switch (role) {
        case 'client':
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 1`,
                elements.page,
                { ignoreElements }
            );

            await browser.scroll('div[data-detail-id="representative"]');
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 2`,
                elements.page,
                { ignoreElements }
            );

            await browser.scroll('div[data-detail-id="account"]');
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 3`,
                elements.page,
                { ignoreElements }
            );
            break;
        case 'admin':
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 1`,
                elements.page,
                { ignoreElements }
            );

            await browser.scroll('div[data-detail-id="representative"]');
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 2`,
                elements.page,
                { ignoreElements }
            );

            await browser.scroll('div[data-detail-id="envelopeAddress"]');
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 3`,
                elements.page,
                { ignoreElements }
            );

            await browser.scroll('div[data-detail-id="authorityDocDetails"]');
            await browser.ybAssertView(
                `new-persons, ${personType} ${text} ${role}, часть 4`,
                elements.page
            );
            break;
        default:
            return 1;
    }
};

module.exports.fillAllFieldsAutoINN = async function (browser, value) {
    await browser.setValue('input[name=inn]', value);
    let valueSelector = `div[data-text="${value}"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);

    await browser.setValue('input[name="phone"]', '12345678');
    await browser.setValue('input[name="email"]', 'test@test.test');

    await browser.setValue('input[name="legal-address-street"]', 'ул уличная');
    await browser.setValue('input[name="legal-address-postcode"]', '142100');
    await browser.setValue('input[name="legal-address-home"]', 'дом для тестировщиков 1');

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Москва');
    valueSelector = `div[data-search="г Москва"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);
    await browser.setValue('input[name="postcode"]', '123456');
    await browser.ybWaitForInvisible('.suggest__spin-container');
    await browser.setValue('input[name="postsuffix"]', 'а/я 123');
};

module.exports.replaceValues = async function (browser, isAdmin = false) {
    await browser.scroll('[name="kpp"]');
    await browser.ybClearValue('[name="kpp"]');
    await browser.setValue('[name="kpp"]', '999999999');
    await browser.ybClearValue('[name="ogrn"]');
    await browser.setValue('[name="ogrn"]', '2127862457380');
    await browser.ybClearValue('[name="phone"]');
    await browser.setValue('[name="phone"]', '88005553535');
    await browser.ybClearValue('[name="email"]');
    await browser.setValue('[name="email"]', 'bonk@bonk.bonk');
    await browser.ybClearValue('[name="fax"]');
    await browser.setValue('[name="fax"]', '89998887766');

    await browser.ybClearValue('[name="representative"]');
    await browser.setValue('[name="representative"]', 'Контактная Личность');

    await browser.ybClearValue('[name="city"]');
    await browser.setValue('[name="city"]', 'Москва');
    let valueSelector = `div[data-search="г Москва"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);

    await browser.setValue('[name="street"]', 'ул Льва Толстого');
    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.ybClearValue('[name="postcode"]');
    await browser.setValue('[name="postcode"]', '111222');
    await browser.ybWaitForInvisible('.suggest__spin-container');

    await browser.ybClearValue('[name="postsuffix"]');
    await browser.setValue('[name="postsuffix"]', '123');

    await browser.ybClearValue('[name="bik"]');
    await browser.setValue('[name="bik"]', '044030653');

    await browser.ybClearValue('[name="account"]');
    await browser.setValue('[name="account"]', '40817810455000000131');

    await browser.setValue('[name="kbk"]', '18210202140061110160');
    await browser.setValue('[name="oktmo"]', '45301000');

    if (isAdmin) {
        await browser.ybClearValue('[name="name"]');
        await browser.setValue('[name="name"]', 'Краткое название органзиации');
        await browser.ybClearValue('[name="longname"]');
        await browser.setValue('[name="longname"]', 'Полное название орг');

        await browser.click('[data-detail-id="countryId"] button');
        await browser.click('span=Абхазия');

        await browser.click('[data-detail-id="reviseActPeriodType"] button');
        await browser.click('span=ежемесячно');

        await browser.ybClearValue('[name="legaladdress"]');
        await browser.setValue('[name="legaladdress"]', 'привет, я адрес');

        await browser.ybClearValue('[name="city"]');
        await browser.setValue('[name="city"]', 'Москва');
        valueSelector = `div[data-search="г Москва"]`;
        await browser.waitForVisible(valueSelector);
        await browser.click(valueSelector);

        await browser.setValue('[name="street"]', 'ул Льва Толстого');
        await browser.ybWaitForInvisible('.suggest__spin-container');

        await browser.ybClearValue('[name="postcode"]');
        await browser.setValue('[name="postcode"]', '111222');
        await browser.ybWaitForInvisible('.suggest__spin-container');

        await browser.ybClearValue('[name="address"]');
        await browser.setValue('[name="address"]', 'ул льва толстого 1111');

        await browser.click('[data-detail-id="deliveryType"] button');
        await browser.click('span=почта');

        await browser.click('[data-detail-id="deliveryCity"] button');
        await browser.click('span=Казань');

        await browser.ybClearValue('[name="signer-person-name"]');
        await browser.setValue('[name="signer-person-name"]', 'фио подписанта');

        await browser.click('[data-detail-id="signerPersonGender"] button');
        await browser.click('span=женский');

        await browser.click('[data-detail-id="signerPositionName"]');
        await browser.click('span=Президент');

        await browser.ybClearValue('[name="authority-doc-details"]');

        await browser.setValue('[name="authority-doc-details"]', 'fdsfdsfdsf');

        await browser.setValue('[name="payment-purpose"]', 'хочу редачить формы');
    }
};

module.exports.openChangePersonForm = async function (browser, personCategory) {
    const { login } = await browser.ybSignIn({
        isAdmin: true,
        isReadonly: false
    });
    await browser.ybRun('create_client_for_user', {
        login
    });

    await browser.ybUrl('user', `new-persons.xml`);

    await browser.waitForVisible(elements.addEmptyPersons);

    await browser.click(elements.addEmptyPersons);

    await browser.ybWaitForLoad();

    await browser.click(elements.personsType.radioButton.ur);

    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

    await browser.click(elements.personsType.listBox);
    await browser.click(personType[personCategory].selector);

    await browser.click(elements.continueButton);

    await browser.waitForVisible(elements.inn);
};

module.exports.fillInn = async function (browser, value) {
    await browser.setValue(elements.inn, value);
    let valueSelector = `div[data-text="${value}"]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);
};
