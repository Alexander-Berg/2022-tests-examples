const common = require('./common');
const tests = require('../tests-generator');
const { elements } = require('../elements');
const { assertViewOpts } = require('../config');

describe('admin', () => {
    describe('change-person', () => {
        tests(common);

        const { personType, partner } = common;
        describe(`${personType}_${partner}`, () => {
            describe(`редактирование`, () => {
                it('с заданным ЮMoney', async function () {
                    const { browser } = this;
                    const signInOpts = { isAdmin: true, isReadonly: false };
                    const { login } = await browser.ybSignIn(signInOpts);

                    const personTypeWithYamoney = `${personType}_with_yamoney`;
                    const { client_id } = await browser.ybRun(
                        'create_client_with_person_for_user',
                        [login, personTypeWithYamoney, partner]
                    );

                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personsList);
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybAssertView(
                        `форма - редактирование, с заданным ЮMoney`,
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с заполненным ЮMoney',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
