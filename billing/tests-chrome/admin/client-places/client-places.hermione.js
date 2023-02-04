const elements = {
    submit: '.yb-form-section form button[type=submit]',
    form: '.yb-form-section form',
    saved: '.yb-client-places__saved',
    select: 'table tr:first-child .yb-client-places__category'
};

const assertOpts = { tolerance: 8, antialiasingTolerance: 10 };

describe('admin', () => {
    describe('client-places', () => {
        it('отображение всех полей', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const client_id = 51730;
            await browser.ybUrl('admin', `client-places.xml?tcl_id=${client_id}`);
            await browser.waitForVisible(elements.form);
            await browser.ybAssertView('все поля', elements.form, assertOpts);
        });

        it('изменение категории', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('create_client_with_places', [login]);
            const url = `client-places.xml?tcl_id=${client_id}`;

            await browser.ybUrl('admin', url);
            await browser.waitForVisible(elements.form);
            await browser.ybSetSteps('выбирает категорию Туризм');
            await browser.ybLcomSelect(elements.select, 'Туризм');
            await browser.ybSetSteps('нажимает Изменить категории');
            await browser.click(elements.submit);
            await browser.waitForVisible(elements.saved);

            await browser.ybUrl('admin', url);
            await browser.waitForVisible(elements.form);
            await browser.ybAssertView('изменение категории', elements.form, {
                ...assertOpts,
                ignoreElements: [`${elements.form} table tr td:first-child`]
            });
        });
    });
});
