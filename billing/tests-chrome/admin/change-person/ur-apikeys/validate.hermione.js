const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');
const { navigateToNewUrApikeysChangePerson } = require('./helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType}`, () => {
            describe('валидация', () => {
                it('обязательные поля', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await navigateToNewUrApikeysChangePerson(browser, client_id);
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        `форма - валидация, обязательные поля`,
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('ИНН, КПП, email', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await navigateToNewUrApikeysChangePerson(browser, client_id);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.inn, false, '12345678901'); // не 10-значным или 12-значным число
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('inn'));
                    await browser.ybAssertView(
                        `поле ИНН, значение несоответствующего формата`,
                        elements.detail('inn'),
                        assertViewOpts
                    );

                    await setValue(browser, details.inn, false, '1234567890');
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('inn'));
                    await browser.ybAssertView(
                        `поле ИНН, значение не является ИНН`,
                        elements.detail('inn'),
                        assertViewOpts
                    );

                    await setValue(browser, details.inn, false, '3358359869');
                    await setValue(browser, { ...details.kpp, value: '11112222' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('kpp'));
                    await browser.ybAssertView(
                        `поле КПП, значение несоответствующего формата`,
                        elements.detail('kpp'),
                        assertViewOpts
                    );

                    await setValue(browser, details.kpp);
                    await setValue(browser, { ...details.email, value: 'это не email' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('email'));
                    await browser.ybAssertView(
                        `поле Email, значение несоответствующего формата`,
                        elements.detail('email'),
                        assertViewOpts
                    );
                });

                it('ОГРН, почтовый индекс', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await navigateToNewUrApikeysChangePerson(browser, client_id);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, { ...details.ogrn, value: '1111122222' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('ogrn'));
                    await browser.ybAssertView(
                        `поле ОГРН, значение несоответствующего формата, длина`,
                        elements.detail('ogrn'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.ogrn, value: '111112222233x' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('ogrn'));
                    await browser.ybAssertView(
                        `поле ОГРН, значение несоответствующего формата, цифры`,
                        elements.detail('ogrn'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.postcodeSimple, value: '12345' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('ogrn'));
                    await browser.ybAssertView(
                        `поле Почт. адрес не по справ. - почтовый индекс, значение несоответствующего формата, длина`,
                        elements.detail('postcodeSimple'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.postcodeSimple, value: '12345x' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('ogrn'));
                    await browser.ybAssertView(
                        `поле Почт. адрес не по справ. - почтовый индекс, значение несоответствующего формата, цифры`,
                        elements.detail('postcodeSimple'),
                        assertViewOpts
                    );
                });

                it('БИК, расчетный счет', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await navigateToNewUrApikeysChangePerson(browser, client_id);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, { ...details.bik, value: '12345678' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('bik'));
                    await browser.ybAssertView(
                        `поле БИК, значение несоответствующего формата, длина`,
                        elements.detail('bik'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.bik, value: '123456789' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('bik'));
                    await browser.ybAssertView(
                        `поле БИК, значение не является БИК`,
                        elements.detail('bik'),
                        assertViewOpts
                    );

                    await setValue(browser, details.bik);
                    await setValue(browser, { ...details.account, value: '1234567890123456789' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('account'));
                    await browser.ybAssertView(
                        `поле РС, значение несоответствующего формата`,
                        elements.detail('account'),
                        assertViewOpts
                    );

                    await setValue(browser, details.bik);
                    await setValue(browser, { ...details.account, value: '12345123451234512345' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('account'));
                    await browser.ybAssertView(
                        'поле РС, значение не является РС',
                        elements.detail('account'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.bik, value: '' });
                    await setValue(browser, details.account);
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('bik'));
                    await browser.ybAssertView(
                        'поле БИК, не заполнен, когда РС заполнен',
                        elements.detail('bik'),
                        assertViewOpts
                    );

                    await setValue(browser, { ...details.bik, value: '123456789' });
                    await setValue(browser, details.account);
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('bik'));
                    await browser.ybAssertView(
                        'поле БИК, значение не является БИК, РС корректный',
                        elements.detail('bik'),
                        assertViewOpts
                    );

                    await setValue(browser, details.bik);
                    await setValue(browser, { ...details.account, value: '' });
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('postbox')); // ждем любую ошибку
                    await browser.ybAssertView(
                        'поле РС, не требовать, если заполнен БИК',
                        elements.detail('account'),
                        assertViewOpts
                    );
                });

                it('запрещенный ИНН, нет права UseRestrictedINN', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.UseRestrictedINN]
                    });
                    const { client_id } = await browser.ybRun('create_client');
                    await navigateToNewUrApikeysChangePerson(browser, client_id);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.inn, false, '7736207543');
                    await browser.waitForVisible('.Suggest-List li:nth-child(1)');
                    await browser.click('.Suggest-List li:nth-child(1)');
                    await browser.click(elements.btnSubmit);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.waitForVisible(elements.detailError('inn'));

                    await browser.ybAssertView('ИНН - запрещенный ИНН', elements.detail('inn'));
                });
            });
        });
    });
});
