const { assertViewOpts } = require('../config');
const { elements } = require('../elements');
const { setSuggestValue, setValue } = require('../helpers');
const { data } = require('./data');

describe('admin', () => {
    describe('change-person', () => {
        describe('ur_0', () => {
            describe('валидация', () => {
                it('Обязательные поля', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=ur&partner=0&client_id=${client_id}`
                    );

                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.error);

                    await browser.ybAssertView(
                        'validate_ur_empty-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('ИНН, КПП, email', async function () {
                    const TIMEOUT = 7500;

                    // соответствует https://testpalm.yandex-team.ru/testcase/balanceassessors-26 (шаги 5+, только на русском)
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=ur&partner=0&client_id=${client_id}`
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" строкой несоответствующего формата (т.е. не 10-значным или 12-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку "Зарегистрировать"`
                    );

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.inn, '55555555555');
                    await browser.setValue(elements.name, 'какая-то контора');
                    await browser.setValue(elements.longname, 'какая-то полная контора');
                    await browser.setValue(elements.kpp, '831344155');
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue(elements.legaladdress, data.add.ur.legaladdress);
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.waitForVisible('[data-detail-id=inn] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'validate_ur_invalid-inn-format',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" числом, не являющимся ИНН (например, 1234567890). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );

                    await browser.setValue(elements.inn, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.inn, '1234567890');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.waitForVisible('[data-detail-id=inn] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'validate_ur_invalid-inn',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" валидным значением (например, 3358359869)`
                    );

                    await browser.setValue(elements.inn, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.inn, '3358359869');
                    await browser.setValue(elements.kpp, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.kpp, '11112222');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.waitForVisible('[data-detail-id=kpp] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'validate_ur_invalid-kpp',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Заполнить поле "КПП" строкой несоответствующего формата (т.е. не 9-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”.
Заполнить поле “Электронная почта” форматом, не соответствующим email’y. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”.`);

                    await browser.setValue(elements.kpp, '111122223');
                    await browser.setValue(elements.email, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.email, 'пишите сюда');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=email] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-email',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('ОГРН, почтовый индекс', async function () {
                    const TIMEOUT = 7500;

                    // соответствует https://testpalm.yandex-team.ru/testcase/balanceassessors-26 (шаги 5+, только на русском)
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=ur&partner=0&client_id=${client_id}`
                    );

                    await browser.ybSetSteps(`Ввести в поля "ИНН" и “КПП” не цифровые символы.
Заполнить поле “ОГРН” числом несоответствующего формата (т.е. не 13-значным или 15-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`);

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.inn, 'TEST');
                    await browser.setValue(elements.kpp, 'TEST');
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.ybAssertView(
                        'validate_ur_inn-kpp-only-digits',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “ОГРН” 13- или 15-значной строкой, состоящей не только из цифр. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );

                    await browser.setValue(elements.ogrn, '11111222223333');
                    await browser.setValue(elements.inn, '3266162051');
                    await browser.setValue(elements.name, 'какая-то контора');
                    await browser.setValue(elements.longname, 'какая-то полная контора');
                    await browser.setValue(elements.kpp, '831344155');
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue(elements.legaladdress, data.add.ur.legaladdress);
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.waitForVisible(
                        '[data-detail-id=ogrn] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-ogrn',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "Почтовый индекс" в блоке "Юридический адрес" числом несоответствующего формата (т.е. не 6-значным числом).`
                    );

                    await browser.setValue(elements.ogrn, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.ogrn, '1234567890xxx');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=ogrn] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-ogrn-digits',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.setValue(elements.ogrn, ['\uE051a', 'Delete']);
                    await browser.click('input[name=legal-addr-type][value="1"]');
                    await setSuggestValue(browser, elements.legalAddressCity, 'химк', 'Химки');
                    await browser.setValue(elements.legalAddressPostcode, '12345');
                    await browser.setValue(elements.legalAddressHome, '5');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=legalAddressPostcode] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-legal-address-postcode',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Почтовый индекс” в блоке “Юридический адрес” 6-значной строкой, состоящей не только из цифр. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );

                    await browser.setValue(elements.legalAddressPostcode, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.legalAddressPostcode, '12345x');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=legalAddressPostcode] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-legal-address-postcode-digits',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.setValue(elements.legalAddressPostcode, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.legalAddressPostcode, '123456');
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.postcodeSimple, '12345');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=postcodeSimple] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-postcode-simple',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await setValue(browser, data.add.ur.city);

                    await browser.ybSetSteps(
                        `Повторить предыдущие 2 шага в поле "Почтовый индекс" блоке "Почтовый адрес" для обоих вариантов доставки.`
                    );

                    // Повторить предыдущие 2 шага в поле "Почтовый индекс" блоке "Почтовый адрес" для обоих вариантов доставки.
                    await browser.setValue(elements.postcodeSimple, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.postcodeSimple, '12345x');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=postcodeSimple] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-postcode-simple-digits',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click('input[name=is-postbox][value="0"]');
                    await setSuggestValue(browser, 'input[name=city]', 'химк', 'Химки');
                    await browser.setValue(elements.postcode, '12345');
                    await browser.setValue(elements.postsuffix, '10');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=postcode] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-postcode',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.setValue(elements.postcode, ['\uE051a', 'Delete']);
                    await browser.setValue(elements.postcode, '12345x');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=postcode] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'validate_ur_invalid-postcode-digits',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                // https://testpalm.yandex-team.ru/testcase/balanceassessors-29
                it('БИК, расчетный счет', async function () {
                    const TIMEOUT = 7500;

                    // соответствует https://testpalm.yandex-team.ru/testcase/balanceassessors-26 (шаги 5+, только на русском)
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=ur&partner=0&client_id=${client_id}`
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом несоответствующего формата (т.е. не 9-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.bik, '12345678');
                    await browser.setValue(elements.inn, '3266162051');
                    await browser.setValue(elements.name, 'какая-то контора');
                    await browser.setValue(elements.longname, 'какая-то полная контора');
                    await browser.setValue(elements.kpp, '831344155');
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue(elements.legaladdress, data.add.ur.legaladdress);
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('.suggest__spin-container', TIMEOUT, true);
                    await browser.waitForVisible('[data-detail-id=bik] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'БИК - число несоответствующего формата',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом, не являющимся БИК (например, 123456789). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue(elements.bik, '123456789');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('[data-detail-id=bik] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'БИК - число не являяетяс БИК',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” числом несоответствующего формата (т.е. не 20-значным числом). Заполнить поле “БИК” корректным значением. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue(elements.bik, '044030001');
                    await browser.ybReplaceValue(elements.account, '1234567890123456789');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=account] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'РС - число несоответствующего формата',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” числом, не являющимся р/с (например, 12345123451234512345). Заполнить поле “БИК” корректным значением. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue(elements.bik, '044030001');
                    await browser.ybReplaceValue(elements.account, '12345123451234512345');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(
                        '[data-detail-id=account] ' + elements.error,
                        TIMEOUT
                    );
                    await browser.ybAssertView(
                        'РС - число не является РС',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” корректным значением. Не заполнять поле “БИК”. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”. Проверяем, что требует заполнить поле “БИК”. Провекра р/с при этом не происходит, поле не подствечивается ошибкой.`
                    );
                    await browser.ybClearValue(elements.bik);
                    await browser.ybReplaceValue(elements.account, '40702810500000000000');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('[data-detail-id=bik] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'РС заполнен, БИК не заполнен',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом, не являющимся БИК (например, 123456789). Заполнить поле “Расчетный счет” корректным значениме. Нажать на кнопку “Зарегистрировать”. Проверяем, что провекра р/с при этом не происходит, поле не подствечивается ошибкой.`
                    );
                    await browser.ybReplaceValue(elements.bik, '123456789');
                    await browser.ybReplaceValue(elements.account, '40702810500000000000');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible('[data-detail-id=bik] ' + elements.error, TIMEOUT);
                    await browser.ybAssertView(
                        'БИК - число не является БИК, РС - корректный',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('Не требовать РС, если заполнен БИК', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, 'ur', '0', true]
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personDetails);
                    await browser.click(elements.editLink);

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.ybReplaceValue(elements.bik, '044030001');
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.ybClearValue(elements.account);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(elements.personsList);
                });
            });
        });
    });
});
