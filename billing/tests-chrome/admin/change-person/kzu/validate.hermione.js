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

                    await setValue(browser, details.name);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.legaladdress);

                    await setValue(browser, { ...details.email, value: 'example.ru' });
                    await setValue(browser, { ...details.rnn, value: '1234567890qw' });
                    await setValue(browser, { ...details.kzIn, value: '1234567890' });
                    await setValue(browser, { ...details.kbe, value: '78' });
                    await setValue(browser, { ...details.bik, value: '12qwerty' });
                    await setValue(browser, { ...details.iik, value: 'qwerty12345' });

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
