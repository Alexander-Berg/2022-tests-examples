const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { details } = require('./details');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe('byu', () => {
            describe('создание', () => {
                it('только обязательные поля', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.byu_0);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.longname);
                    await setValue(browser, details.legaladdress);

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

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.byu_0);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.fax);
                    await setValue(browser, details.representative);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.invalidAddress);
                    await setValue(browser, details.invalidBankprops);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.longname);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.benBank);
                    await setValue(browser, details.swift);
                    await setValue(browser, details.account);
                    await setValue(browser, details.deliveryType);
                    await setValue(browser, details.deliveryCity);
                    await setValue(browser, details.liveSignature);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);
                    await setValue(browser, details.vip);
                    await setValue(browser, details.earlyDocs);

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

                it('нет права PersonPostAddressEdit', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [Perms.NewUIEarlyAdopter],
                        exclude: [Perms.PersonPostAddressEdit]
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=byu&partner=0&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybAssertView(
                        'форма - создание, нет права PersonPostAddressEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('нет права PersonExtEdit', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonExtEdit]
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=byu&partner=0&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybAssertView(
                        'форма - создание, нет права PersonExtEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
