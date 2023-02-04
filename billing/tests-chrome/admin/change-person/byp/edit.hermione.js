const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(`${personType} ${partner}`, () => {
            describe('редактирование', () => {
                it('редактирует поле lname и проставляет чек-бокс "Невалидный адрес"', async function () {
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

                    await setValue(browser, {
                        ...details.lname,
                        value: 'rename'
                    });
                    await setValue(browser, details.invalidAddress);

                    await browser.ybAssertView(
                        'форма - редактирование, изменено lname и проставлен чек-бокс "Невалидный адрес"',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с измененным полем lname и проставленным чек-боксом "Невалидный адрес"',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
                it('редактирование, нет права PersonPostAddressEdit', async function () {
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

                    await browser.ybAssertView(
                        'форма - редактирование, нет права PersonPostAddressEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
