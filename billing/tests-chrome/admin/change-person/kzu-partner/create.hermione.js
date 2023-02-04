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
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.kzIn);
                    await setValue(browser, details.kbe);
                    await setValue(browser, details.bik);
                    await setValue(browser, details.iik);

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
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.city);
                    await setValue(browser, details.localCity);
                    await setValue(browser, details.postaddress);
                    await setValue(browser, details.localPostaddress);
                    await setValue(browser, details.invalidBankprops);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.localLegaladdress);
                    await setValue(browser, details.rnn);
                    await setValue(browser, details.kzIn);
                    await setValue(browser, details.kbe);
                    await setValue(browser, details.bik);
                    await setValue(browser, details.bank);
                    await setValue(browser, details.corrSwift);
                    await setValue(browser, details.localBank);
                    await setValue(browser, details.iik);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.localSignerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.localSignerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);
                    await setValue(browser, details.localAuthorityDocDetails);

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

                it('нет права LocalNamesMaster', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.LocalNamesMaster]
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personType, partner]
                    );

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=${personType}&partner=${partner}&client_id=${client_id}`
                    );
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybSetSteps('Проверяет, что не видны локальные названия');
                    await browser.ybAssertView(
                        'форма - создание, нет права LocalNamesMaster - не видны локальные названия',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
