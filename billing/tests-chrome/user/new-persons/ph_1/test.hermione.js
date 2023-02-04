const { personType } = require('../common');

const { elements, ignoreElements } = require('../elements');

const { takeScreenshots, replaceValues } = require('./helpers');

describe('user', () => {
    describe('new-persons', () => {
        describe(`${personType.ph_1.name}`, () => {
            describe('Просмотр', () => {
                describe('админ', () => {
                    it('просмотр карточки плательщика', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: true,
                            isReadonly: false
                        });
                        const { person_id } = await browser.ybRun(
                            'create_client_with_person_for_user',
                            {
                                login,
                                person_type: 'ph',
                                is_partner: true
                            }
                        );

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.ybSetSteps(`Выбираем партнера`);
                        await browser.click('[value="partner"]');

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.ybUrl('user', `new-persons.xml#/${person_id}`);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_1.name} карточка плательщика админ часть 1`,
                            elements.page
                        );

                        await browser.scroll('div=Адрес');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_1.name} карточка плательщика админ часть 2`,
                            elements.page
                        );
                    });
                });
            });
        });
    });
});
