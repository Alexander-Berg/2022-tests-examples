const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { setValue } = require('../helpers');
const { changePersonTypeToPhAutoru } = require('./helpers');
const { personType, details } = require('./common');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('создание', () => {
                it('только обязательные поля', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ph_0']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToPhAutoru(browser);

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.mname);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.agree);

                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с обязательными полями',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('все поля', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ph_0']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToPhAutoru(browser);

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.mname);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.fax);
                    await setValue(browser, details.email);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.invalidAddress);
                    await setValue(browser, details.agree);

                    await browser.ybAssertView(
                        'форма - создание, заполнены все поля',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика со всеми полями',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
