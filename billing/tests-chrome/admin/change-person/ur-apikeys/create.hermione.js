const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { setValue } = require('../helpers');
const { changePersonTypeToUrApikeys } = require('./helpers');
const { personType, details } = require('./common');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('создание', () => {
                it('только обязательные поля, адрес по справочнику', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ur']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToUrApikeys(browser);

                    await setValue(browser, details.inn);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);

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

                it('только обязательные поля, адрес без справочника', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ur']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToUrApikeys(browser);

                    await setValue(browser, details.inn, false, '3266162051');
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await setValue(browser, details.name, false, 'какая-то контора');
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await setValue(browser, details.longname);
                    await setValue(browser, details.kpp);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);

                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля, адреса без справочника',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с обязательными полями, адреса без справочника',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('все поля, адрес по справочнику', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ur']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToUrApikeys(browser);

                    await setValue(browser, details.inn);
                    await setValue(browser, details.kpp);
                    await setValue(browser, details.ogrn);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.fax);
                    await setValue(browser, details.representative);
                    await setValue(browser, details.reviseActPeriodType);
                    await setValue(browser, details.invalidAddress);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);
                    await setValue(browser, details.invalidBankprops);
                    await setValue(browser, details.bik);
                    await setValue(browser, details.account);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);
                    await setValue(browser, details.address);
                    await setValue(browser, details.deliveryType);
                    await setValue(browser, details.deliveryCity);
                    await setValue(browser, details.liveSignature);
                    await setValue(browser, details.vip);

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

                it('только обязательные поля, нет права PersonPostAddressEdit, адрес по справочнику', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    });

                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ur']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToUrApikeys(browser);

                    await setValue(browser, details.inn);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);
                    await browser.click('.Radiobox-Box');

                    await browser.ybSetSteps('Проверяет, что не отображаются поля delivery-city');
                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля - нет права PersonPostAddressEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                });

                it('только обязательные поля, ИП', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu['ur']);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await changePersonTypeToUrApikeys(browser);

                    await setValue(browser, {
                        ...details.inn,
                        suggestValue: '503612526896',
                        value: 'Кукушкин'
                    });
                    await setValue(browser, details.phone);
                    await setValue(browser, details.legalAddressCity);
                    await setValue(browser, details.legalAddressStreet);
                    await setValue(browser, details.legalAddressPostcode);
                    await setValue(browser, details.legalAddressHome);
                    await setValue(browser, details.city);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);
                    await setValue(browser, details.signerPersonName);
                    await setValue(browser, details.signerPersonGender);
                    await setValue(browser, details.signerPositionName);
                    await setValue(browser, details.authorityDocType);
                    await setValue(browser, details.authorityDocDetails);

                    await browser.ybAssertView(
                        'форма - создание, только обязательные поля, ИП',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика, только обязательные поля, ИП',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
