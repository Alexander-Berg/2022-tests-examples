const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('редактирование', () => {
                it('редактирует поле name', async function () {
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

                    await setValue(browser, details.name, true);

                    await browser.ybSetSteps('Проверяет блокировку полей');
                    await browser.ybAssertView(
                        'форма - редактирование, изменено name',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с измененным полем name',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('нет права PersonPostAddressEdit', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    });

                    const { person_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personType, partner]
                    );

                    await browser.ybUrl('admin', `change-person.xml?person_id=${person_id}`);
                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.ybSetSteps(
                        'Проверяет, что задизейблены "Название организации", "Полное название организации", "Почтовый адрес", "Почтовый индекс", "Страна", "Номер свидетельства плательщика НДС", "Юридический адрес"'
                    );
                    await browser.ybAssertView(
                        'форма - редактирование, нет права PersonPostAddressEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('нет права UseAdminPersons', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.UseAdminPersons]
                    });

                    await browser.ybRun('create_client_for_user', [login]);

                    const { person_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personType, partner]
                    );

                    await browser.ybUrl('admin', `change-person.xml?person_id=${person_id}`);
                    await browser.ybSetSteps('Проверяет, что отобразилась форма');
                    await browser.waitForVisible(elements.formChangePerson);
                });
            });
        });
    });
});
