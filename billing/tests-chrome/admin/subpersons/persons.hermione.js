const assert = require('chai').assert;

const { elements } = require('./elements');
const { assertViewOpts, assertViewOptsExport } = require('./config');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', () => {
    describe('subpersons', () => {
        describe('persons', () => {
            const categories = [
                'kzu',
                'kzu-partner',
                'ph',
                'ph-partner',
                'ur',
                'ur-partner',
                'yt',
                'yt-partner',
                'eu_yt',
                'eu_yt-partner',
                'sw_ur',
                'sw_ur-partner',
                'il_ur',
                'il_ur-partner'
            ];

            for (let cat of categories) {
                it(cat, async function () {
                    const { browser } = this;

                    await browser.ybSetSteps(
                        `Открываем subpersons.xml. Проверяем полный списков полей плательщика`
                    );

                    const [type, partner] = cat.split('-');

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, type, partner === 'partner' ? '1' : '0', true]
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        `persons_${cat}`,
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            }

            describe('несколько плательщиков у одного клиента', () => {
                it('ur, ph', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_ur_and_ph_persons'
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - список плательщиков - ur, ph',
                        elements.personsList,
                        assertViewOpts
                    );
                });

                it('byu, sw_yt, sw_ytph', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_buy_and_sw_yt_and_sw_ytph_persons'
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - список плательщиков - byu, sw_yt, sw_ytph',
                        elements.personsList,
                        assertViewOpts
                    );
                });
            });

            describe('архивация', () => {
                it('заархивировать и разархивировать', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, 'ur', '0']
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);

                    await browser.ybSetSteps('Нажимаем на Заархивировать');
                    await browser.click(elements.archiveLink);
                    await browser.ybMessageAccept();
                    await browser.waitForVisible(elements.personsFilter);
                    await browser.ybWaitForInvisible(elements.personsFilterDisabled);

                    await browser.ybSetSteps('Нажимаем на чекбокс просмотра заархивированнаых');
                    await browser.click(elements.archivedCheckbox);
                    await browser.waitForVisible(elements.unarchiveLink);

                    await browser.ybSetSteps(
                        'Проверяем, что отображается заархивированный плательщик'
                    );
                    await browser.ybAssertView(
                        `persons_archived`,
                        elements.personsList,
                        assertViewOpts
                    );

                    await browser.ybSetSteps('Нажимаем на Разархивировать');
                    await browser.click(elements.unarchiveLink);
                    await browser.ybMessageAccept();
                    await browser.waitForVisible(elements.personsFilter);
                    await browser.ybWaitForInvisible(elements.personsFilterDisabled);

                    await browser.ybSetSteps(
                        'Проверяем, что отображается разархивированный плательщик'
                    );
                    await browser.ybAssertView(
                        `persons_unarchived`,
                        elements.personsList,
                        assertViewOpts
                    );
                });

                it('автоовердрафт', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun('create_client_with_autooverdraft');

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);

                    await browser.ybAssertView(
                        'плательщик - автоовердрафт - архивация',
                        elements.personId,
                        assertViewOpts
                    );
                    await browser.ybSetSteps('Нажимаем на Заархивировать');
                    await browser.click(elements.archiveLink);
                    const text = await browser.ybMessageGetText();
                    const search = 'Архивация запрещена. Плательщик подписан на автоовердрафт.';
                    assert(text.indexOf(search) >= 0, `Алерт содержит текст '${search}'`);
                });
            });

            it('экспорт', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                const {
                    client_id,
                    person_id
                } = await browser.ybRun('create_client_with_person_for_user', [login, 'ur', '0']);

                await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                await browser.waitForVisible(elements.personsList);

                await browser.ybSetSteps('Проверяем, плательщик поставлен в очередь выгрузки');
                await browser.waitForVisible(elements.reexportWaiting);
                await browser.ybAssertView(
                    `блок экспорта плательщика - проставлен в очередь`,
                    elements.personsList,
                    assertViewOptsExport
                );

                await browser.ybSetSteps('Обновляем статус экспорта плательщика в базе');
                const { exported } = await browser.ybRun('export_person', [person_id]);
                assert(exported, 'Плательщик выгружен');
                await browser.waitForVisible(elements.exported);
                await browser.ybAssertView(
                    `блок экспорта плательщика - выгружен`,
                    elements.personsList,
                    assertViewOptsExport
                );

                await browser.ybSetSteps('Нажимаем на кнопку Перевыгрузить');
                await browser.click(elements.reexportBtn);
                await browser.waitForVisible(elements.reexportWaiting);
                await browser.ybAssertView(
                    `блок экспорта плательщика - проставлен в очередь еще раз`,
                    elements.personsList,
                    assertViewOptsExport
                );
                await browser.ybSetSteps('Обновляем статус экспорта плательщика в базе еще раз');
                const { exported: exported2 } = await browser.ybRun('export_person', [person_id]);
                assert(exported2, 'Плательщик выгружен еще раз');
                await browser.ybWaitForInvisible('.yb-person-reexport-state_waiting');
                await browser.ybAssertView(
                    `блок экспорта плательщика - выгружен еще раз`,
                    elements.personsList,
                    assertViewOptsExport
                );
            });

            it('проверка прав - есть и нет права PersonEdit', async function () {
                const { browser } = this;

                {
                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, 'ph', '0']
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);

                    await browser.click('.yb-create-person__category button');
                    await browser.click('.Popup2 .Menu-Item:first-child');
                    await browser.ybAssertView(
                        `блок создания плательщика - клик на первый элемент списка`,
                        '.yb-create-person',
                        assertViewOptsExport
                    );
                }

                {
                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [Perms.NewUIEarlyAdopter],
                        exclude: [Perms.PersonEdit, Perms.BillingSupport]
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, 'ph', '0']
                    );

                    await browser.ybSetSteps(
                        `Создаем палтельщика ur и открываем subpersons.xml. Проверяем, что блока создания плательщика нет на странице`
                    );
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.ybWaitForInvisible('.yb-create-person');
                }
            });

            it('отображение локальных полей плательщика, если нет права LocalNamesMaster', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [Perms.NewUIEarlyAdopter],
                    exclude: [Perms.PersonEdit, Perms.BillingSupport]
                });

                const { client_id } = await browser.ybRun('create_client_with_person_for_user', [
                    login,
                    'il_ur',
                    '0'
                ]);

                // создание
                await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                await browser.waitForVisible(elements.personDetails);
                await browser.ybAssertView(
                    'список плательщиков - il_ur, нет права LocalNamesMaster',
                    elements.personDetails,
                    assertViewOpts
                );
            });
        });
    });
});
