const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { setValue } = require('../helpers');
const { partner, personType, details } = require('./common');

const validateElements = {
    email: '[data-detail-id="email"]'
};

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('валидация', () => {
                it('валидация email', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    let value = '111';

                    await setValue(browser, details.email, false, value);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(`${validateElements.email} ${elements.error}`);
                    await browser.ybAssertView(
                        `email - значение ${value}`,
                        validateElements.email,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
