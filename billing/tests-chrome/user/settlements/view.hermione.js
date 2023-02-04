describe('user', () => {
    describe('settlements', () => {
        it('акт сверки онлайн, нет актов', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });

            await browser.ybUrl('user', `settlements.xml`);
            await browser.waitForVisible('.yb-user-content');

            await browser.ybAssertView(
                'просмотр акта сверки онлайн, нет актов',
                '.yb-user-content'
            );
        });

        it('акт сверки онлайн, есть акты, проверка ссылок', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-hermione-ci-3' });
            await browser.ybUrl('user', `settlements.xml`);
            await browser.waitForVisible('.yb-user-content');

            await browser.ybAssertView(
                'просмотр акта сверки онлайн, есть акты',
                '.yb-user-content'
            );

            // ничего не хайжу в тесте, тк переналивок не предвидится.
            // если они все-таки случатся, прошу прощения у тестировщиков биллинга из будущего
            await browser.ybAssertLink('a=ссылке', '/get-revise-act.xml?iframe');
        });

        it('акт сверки онлайн, предупреждения о входе под логином без клиента', async function () {
            const { browser } = this;
            await browser.ybSignIn({ login: 'yb-static-balance-5' });
            await browser.ybUrl('user', `settlements.xml`);
            await browser.waitForVisible('.yb-notification_type_error');
        });
    });
});
