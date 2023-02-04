const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('редактирование', () => {
                it(`редактирует поле lname`, async function () {
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

                    await setValue(browser, details.lname, true);

                    await browser.ybAssertView(
                        `форма - редактирование, изменено lname`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с измененным полем lname`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it(`редактирует поле mname, без отчества`, async function () {
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

                    await browser.click('div[data-detail-id="mname"] input[type=checkbox]');

                    await browser.ybAssertView(
                        `форма - редактирование, изменено mname, без отчества`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с измененным полем mname, без отчества`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('редактирует поле bankType', async function () {
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

                    await setValue(browser, { ...details.bankType, value: 'Webmoney' });
                    await setValue(browser, details.webmoneyWallet);

                    await browser.ybAssertView(
                        `форма - редактирование, изменено bankType`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с измененным полем bankType`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it(`удаляет поле "account", нет права PersonPostAddressEdit`, async function () {
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

                    await setValue(browser, details.account, true);

                    await browser.ybAssertView(
                        `форма - редактирование, нет права PersonPostAddressEdit, удалено поле "account"`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с удалённым полем "account", нет права PersonPostAddressEdit`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it(`редактирует поле banktype на СБП`, async function () {
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

                    await setValue(browser, details.bankType);
                    await setValue(browser, details.fpsPhone);
                    await browser.scroll('div[data-detail-id="fpsBank"]');
                    await setValue(browser, details.fpsBank);

                    await browser.ybAssertView(
                        `редактирует поле banktype на СБП`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        `subpersons.xml - карточка плательщика с полем banktype = СБП`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
