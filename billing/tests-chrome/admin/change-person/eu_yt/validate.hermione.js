const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { personType, partner } = require('./common');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
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

                    await browser.waitForVisible(
                        '.src-common-presentational-components-Field-___field-module__detail__error'
                    );

                    await browser.ybAssertView(
                        'форма - валидация, не заполнены обязательные поля',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
