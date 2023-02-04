const { elements } = require('../../../tests-chrome/admin/change-person/elements');
const { assertViewOpts } = require('../../../tests-chrome/admin/change-person/config');

describe('admin', () => {
    describe('change-person', () => {
        describe('Редактирование', () => {
            // не стабильный из-за анимации
            it.skip('type=ur, partner=0, role=0; редактирование - отображение подсказок', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });
                const {
                    client_id,
                    person_id
                } = await browser.ybRun('create_client_with_person_for_user', [login, 'ur', '0']);
                await browser.ybUrl(
                    'admin',
                    `change-person.xml?person_id=${person_id}&client_id=${client_id}`
                );

                await browser.ybSetSteps(`Наводит курсор на подсказку для поля Электронная почта.`);

                await browser.waitForVisible(elements.formChangePerson);
                await browser.moveToObject('[data-detail-id="email"] img');
                // TODO: отвязать класс лего, использовать className для Popup
                await browser.waitForVisible('.Popup2');
                const opts = {
                    ...assertViewOpts,
                    ignoreElements: [
                        ...assertViewOpts.ignoreElements,
                        '[data-detail-id=inn]',
                        '[data-detail-id=name]',
                        '[data-detail-id=account]',
                        '.src-common-presentational-components-Field-___field-module__detail__tip'
                    ]
                };
                await browser.ybAssertView('edit_ur-hint', elements.formChangePerson, opts);
            });
        });
    });
});
