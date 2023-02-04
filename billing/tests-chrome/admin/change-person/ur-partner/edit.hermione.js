const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('редактирование', () => {
                it(`редактирует поля название, юр. адрес - по справочнику, почт. адрес - не по справочинку`, async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personType, partner]
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.name, false, 'какая-то контора');
                    await setValue(browser, details.legalAddrType);
                    await setValue(browser, details.legalAddressCity);
                    await setValue(browser, details.legalAddressStreet);
                    await setValue(browser, details.legalAddressPostcode);
                    await setValue(browser, details.legalAddressHome);
                    await setValue(browser, details.isPostbox, true);
                    await setValue(browser, details.postcodeSimple);
                    await setValue(browser, details.postbox);

                    await browser.ybWaitForInvisible(elements.suggestSpin);

                    await browser.ybAssertView(
                        `форма - редактирование, изменены название, юр. адрес, почт. адрес`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с измененными полями название, юр. адрес, почт. адрес`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('самозанятый', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const [client_id] = await browser.ybRun('test_selfemployed_ur_person');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.lname, true);
                    await setValue(browser, details.fname, true);
                    await setValue(browser, details.mname, true);
                    await setValue(browser, details.bankType);
                    await setValue(browser, details.fpsPhone);
                    await setValue(browser, details.fpsBank);

                    await browser.ybAssertView(
                        `форма - редактирование самозанятого, изменены ФИО`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка самозанятого плательщика с измененными ФИО`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it(`редактирует поле phone, нет права PersonPostAddressEdit`, async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    });
                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personType, partner]
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.phone, true);

                    await browser.ybAssertView(
                        `форма - редактирование, нет права PersonPostAddressEdit, изменено phone`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с измененным полем phone, нет права PersonPostAddressEdit`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('создание / редактирование с галкой "Почтовый адрес совпадает с юридическим"', async function () {
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

                    await setValue(browser, details.inn);
                    await setValue(browser, details.name);
                    await setValue(browser, details.email);
                    await setValue(browser, details.phone);
                    await setValue(browser, details.legalAddressCity);
                    await setValue(browser, details.legalAddressStreet);
                    await setValue(browser, details.legalAddressPostcode);
                    await setValue(browser, details.legalAddressHome);

                    await setValue(browser, details.isPostbox);
                    await setValue(browser, details.isSamePostaddress);

                    await browser.ybAssertView(
                        'форма - создание, копируется почтовый адрес (до сохранения)',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);

                    await browser.click(elements.editLink);

                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybAssertView(
                        'форма - редактирование, скопирован почтовый адрес (после сохранения)',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await setValue(browser, details.isSamePostaddress);

                    await browser.ybAssertView(
                        'форма - редактирование, галка копирования адреса снята',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
