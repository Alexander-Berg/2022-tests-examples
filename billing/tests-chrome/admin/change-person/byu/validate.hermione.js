const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { setValue } = require('../helpers');
const { details } = require('./details');

const validateElements = {
    postcode: '[data-detail-id="postcode"]',
    inn: '[data-detail-id="inn"]'
};

describe('admin', () => {
    describe('change-person', () => {
        describe('byu', () => {
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
                        `change-person.xml?type=byu&partner=0&client_id=${client_id}`
                    );

                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(
                        '.src-common-presentational-components-Field-___field-module__detail__error'
                    );

                    await browser.ybAssertView(
                        'форма - валидация, не заполнены обязательные поля',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('Почтовый индекс - ровно 6 цифр', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=byu&partner=0&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    let value = '111';
                    await setValue(browser, details.postcode, false, value);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(`${validateElements.postcode} ${elements.error}`);
                    await browser.ybAssertView(
                        `почтовый индекс - значение ${value}`,
                        validateElements.postcode,
                        assertViewOpts
                    );

                    value = '111xxx';
                    await setValue(browser, details.postcode, false, value);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(`${validateElements.postcode} ${elements.error}`);
                    await browser.ybAssertView(
                        `почтовый индекс - значение ${value}`,
                        validateElements.postcode,
                        assertViewOpts
                    );
                });

                it('Учетный номер плательщика - ровно 9 цифр', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=byu&partner=0&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    let value = 'xxx';
                    await setValue(browser, details.inn, false, value);
                    await browser.ybAssertView(
                        `учетный номер плательщика - значение ${value} - не дает вводить не цифры`,
                        validateElements.inn,
                        assertViewOpts
                    );

                    value = '111';
                    await setValue(browser, details.inn, false, value);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(`${validateElements.inn} ${elements.error}`);
                    await browser.ybAssertView(
                        `учетный номер плательщика - значение ${value}`,
                        validateElements.inn,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
