const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { details, personType, partner } = require('./common');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
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
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.payType);
                    await setValue(browser, details.account);
                    await setValue(browser, details.swift);
                    await setValue(browser, details.corrSwift);

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
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name);
                    await setValue(browser, details.longname);
                    await setValue(browser, details.localName);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.fax);
                    await setValue(browser, details.email);
                    await setValue(browser, details.representative);
                    await setValue(browser, details.localRepresentative);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.localPostaddress);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.invalidAddress);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.invalidBankprops);
                    await setValue(browser, { ...details.payType, value: 'IBAN' });
                    await setValue(browser, details.iban);
                    await setValue(browser, details.swiftOpt);
                    await setValue(browser, details.corrSwiftOpt);
                    await setValue(browser, details.benBank);
                    await setValue(browser, details.localBenBank);

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

                it('только обязательные поля, нет права LocalNamesMaster', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.LocalNamesMaster]
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.payType);
                    await setValue(browser, details.account);
                    await setValue(browser, details.swift);
                    await setValue(browser, details.corrSwift);

                    await browser.ybSetSteps('Проверяет, что не отображаются поля local* поля');
                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля - нет права LocalNamesMaster',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                });
            });
        });
    });
});
