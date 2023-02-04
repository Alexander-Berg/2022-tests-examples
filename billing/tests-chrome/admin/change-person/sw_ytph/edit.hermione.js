const { elements } = require('../elements');
const { assertViewOpts } = require('../config');
const { partner, personType, details } = require('./common');
const { setValue } = require('../helpers');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('редактирование', () => {
                it('редактирует поле phone', async function () {
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

                    await browser.ybAssertView(
                        'форма - редактирование, документы проверены',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await setValue(browser, details.verifiedDocs, true);

                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(elements.personsList);

                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await setValue(browser, details.phone, true);

                    await browser.ybAssertView(
                        'форма - редактирование, изменено phone',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personsList);
                    await browser.ybAssertView(
                        'subpersons.xml - карточка плательщика с измененным полем phone',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
