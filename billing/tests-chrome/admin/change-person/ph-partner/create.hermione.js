const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { setValue } = require('../helpers');
const { partner, personType, details } = require('./common');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('создание', () => {
                it('только обязательные поля, адреса по справочнику', async function () {
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

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.birthday);
                    await setValue(browser, details.passportS);
                    await setValue(browser, details.passportN);
                    await setValue(browser, details.passportD);
                    await setValue(browser, details.passportE);
                    await setValue(browser, details.passportCode);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.isPostbox);
                    await setValue(browser, details.city);
                    await setValue(browser, details.street);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.postsuffix);
                    await setValue(browser, details.legalAddrType);
                    await setValue(browser, details.legalAddressCity);
                    await setValue(browser, details.legalAddressStreet);
                    await setValue(browser, details.legalAddressPostcode);
                    await setValue(browser, details.legalAddressHome);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.pfr);
                    await setValue(browser, details.bankType);
                    await setValue(browser, details.fpsPhone);
                    await setValue(browser, details.fpsBank);

                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля, адреса по справочнику',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с обязательными полями, адреса по справочнику',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('только обязательные поля, адреса без справочника', async function () {
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

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.birthday);
                    await setValue(browser, details.passportS);
                    await setValue(browser, details.passportN);
                    await setValue(browser, details.passportD);
                    await setValue(browser, details.passportE);
                    await setValue(browser, details.passportCode);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.isPostbox);
                    await setValue(browser, details.city);
                    await setValue(browser, details.street);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.postsuffix);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.pfr);
                    await setValue(browser, details.bankType);
                    await setValue(browser, details.fpsPhone);
                    await setValue(browser, details.fpsBank);

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
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.mname);
                    await setValue(browser, details.birthday);
                    await setValue(browser, details.passportBirthplace);
                    await setValue(browser, details.birthplaceDistrict);
                    await setValue(browser, details.birthplaceRegion);
                    await setValue(browser, details.birthplaceCountry);
                    await setValue(browser, details.passportS);
                    await setValue(browser, details.passportN);
                    await setValue(browser, details.passportD);
                    await setValue(browser, details.passportE);
                    await setValue(browser, details.passportCode);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.countryId);
                    await setValue(browser, details.invalidAddress);
                    await setValue(browser, details.deliveryType);
                    await setValue(browser, details.deliveryCity);
                    await setValue(browser, details.isPostbox);
                    await setValue(browser, details.city);
                    await setValue(browser, details.street);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.postsuffix);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.bik);
                    await setValue(browser, details.pfr);
                    await setValue(browser, details.account);
                    await setValue(browser, { ...details.bankType, value: 'сбербанк' });
                    await setValue(browser, details.personAccount);
                    await setValue(browser, details.bankInn);
                    await setValue(browser, details.paymentPurpose);

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

                it('только обязательные поля, нет права PersonPostAddressEdit', async function () {
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
                    await browser.click(elements.menu[`${personType}_${partner}`]);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.lname);
                    await setValue(browser, details.fname);
                    await setValue(browser, details.birthday);
                    await setValue(browser, details.passportS);
                    await setValue(browser, details.passportN);
                    await setValue(browser, details.passportD);
                    await setValue(browser, details.passportE);
                    await setValue(browser, details.passportCode);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.email);
                    await setValue(browser, details.isPostbox);
                    await setValue(browser, details.city);
                    await setValue(browser, details.street);
                    await setValue(browser, details.postcode);
                    await setValue(browser, details.postsuffix);
                    await setValue(browser, details.legaladdress);
                    await setValue(browser, details.inn);
                    await setValue(browser, details.pfr);
                    await setValue(browser, { id: 'bankType', type: 'select', value: 'Webmoney' });

                    await browser.ybSetSteps(
                        'Проверяет, что не отображаются поля delivery-type, delivery-city, *-wallet, payment-purpose'
                    );
                    await browser.ybAssertView(
                        'форма - создание, заполнены обязательные поля - нет права PersonPostAddressEdit',
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
