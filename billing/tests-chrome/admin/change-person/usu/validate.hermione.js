const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('валидация', () => {
                it('обязательные поля', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=${personType}&partner=${partner}&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        `форма - валидация, обязательные поля`,
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
                it('ограничение на ввод кириллицы', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, {
                        ...details.name,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.representative,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.phone,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.email,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.postaddress,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.city,
                        value: 'раз'
                    });
                    await setValue(browser, {
                        ...details.postcode,
                        value: 'раз'
                    });

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        `форма - ограничение на ввод кириллицы`,
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
