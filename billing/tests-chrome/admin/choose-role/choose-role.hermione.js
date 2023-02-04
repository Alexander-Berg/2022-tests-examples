const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', () => {
    describe('choose-role', () => {
        it('Проверяет переход на страницу клиента со страницы choose-role.xml', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'choose-role.xml?passport-id=1073930354');
            await browser.ybWaitForLoad();
            await browser.ybAssertLink('*=ВИ ИМХО', 'tclient.xml?tcl_id=393872');
        });

        it('Отображение данных в блоке информации пользователя, несколько ролей', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            const { passport_id } = await browser.ybRun('create_full_passport');
            await browser.ybUrl('admin', `choose-role.xml?passport-id=${passport_id}`);
            await browser.ybWaitForLoad();
            await browser.ybAssertView(
                'Отображение данных в блоке информации пользователя, несколько ролей',
                '.yb-content',
                {
                    hideElements: [
                        '.yb-passport-card__client',
                        '.yb-passport-card__email',
                        '.yb-passport-card__login',
                        '.yb-passport-card__passport-id'
                    ]
                }
            );
        });

        it('Пользователь без имени, без email', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            const { passport_id } = await browser.ybRun('create_empty_passport');
            await browser.ybUrl('admin', `choose-role.xml?passport-id=${passport_id}`);
            await browser.ybWaitForLoad();
            await browser.ybAssertView('Пользователь без имени и без email', '.yb-content', {
                hideElements: ['.yb-passport-card__passport-id', '.yb-passport-card__login']
            });
        });

        it('Нет права ViewRoles', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                baseRole: Roles.Support,
                include: [],
                exclude: [Perms.ViewRoles]
            });
            await browser.ybUrl('admin', 'choose-role.xml?passport-id=1073930354');
            await browser.ybWaitForLoad();
            await browser.ybAssertView('Отображение без права ViewRoles', '.yb-content');
        });
    });
});
