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

                    await setValue(browser, { ...details.name, value: 'привет' });
                    await setValue(browser, { ...details.email, value: 'example.ru' });
                    await setValue(browser, { ...details.tvaNumber, value: '1234567890' });
                    await setValue(browser, { ...details.inn, value: '1234567890qw' });

                    await browser.click(elements.btnSubmit);
                    await browser.ybAssertView(
                        `форма - валидация, некорректные значения`,
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
