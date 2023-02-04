const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { details, partner, personType } = require('./common');
const { setValue } = require('../helpers');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('валидация', () => {
                it('обязательные поля', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
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

                it('обязательные поля, вид счета', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=${personType}&partner=${partner}&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, { ...details.payType, value: 'IBAN' });

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        `форма - валидация, обязательные поля, вид счета`,
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('некорректные значения', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=${personType}&partner=${partner}&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, { ...details.email, value: 'example.com' });

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        'форма - валидация, некорректные значения',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
