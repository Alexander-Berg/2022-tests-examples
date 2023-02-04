const { personType } = require('../common');

const { elements, ignoreElements } = require('../elements');

const { takeScreenshots, replaceValues } = require('./helpers');

describe('user', () => {
    describe('new-persons', () => {
        describe(`${personType.ur_1.name}`, () => {
            describe('админ', () => {
                describe('Просмотр', () => {
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
                                person_type: 'ur',
                                is_partner: true
                            }
                        );

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.click('[value="partner"]');

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.ybUrl('user', `new-persons.xml#/${person_id}`);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_1.name} карточка плательщика админ часть 1`,
                            elements.page
                        );

                        await browser.scroll('div=Платежные реквизиты и дополнительная информация');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_1.name} карточка плательщика админ часть 2`,
                            elements.page
                        );
                    });
                });
            });
        });
    });
});
