const { hideElements } = require('./helpers');

describe('user', () => {
    describe('orders', () => {
        it('нет заказов', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });

            await browser.ybUrl('user', `orders.xml`);
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказов, нет заказов', '.content');
        });

        it('пагинация', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({ login: 'yb-hermione-ci-2' });

            await browser.ybUrl('user', `orders.xml`);
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказов, первая страница', '.content', {
                hideElements
            });

            await browser.click('.pages a');
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказов, вторая страница', '.content', {
                hideElements
            });

            await browser.selectByValue('select[name="ps"]', '20');
            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказов, 20 строк', '.content', {
                hideElements
            });
        });

        it('поиск по сервису и сортировка', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({ login: 'yb-hermione-ci-2' });

            await browser.ybUrl('user', `orders.xml`);
            await browser.waitForVisible('.content');

            await browser.selectByValue('select[name="service_group_id"]', '7');
            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы заказов, поиск по сервису', '.content', {
                hideElements
            });

            await browser.click('.report th:nth-child(5) a');
            await browser.waitForVisible('.content');
            await browser.click('.report th:nth-child(5) a');
            await browser.ybAssertView(
                'просмотр страницы заказов, сортировка по дате по убыванию',
                '.content',
                { hideElements }
            );
        });

        it('поиск по субклиенту', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('test_agency_multiple_clients_order', { login });

            await browser.ybUrl('user', `orders.xml`);
            await browser.waitForVisible('.content');

            await browser.ybAssertView('просмотр страницы заказов, под агентством', '.content', {
                hideElements
            });

            await browser.setValue('input[name="subclient_login"]', 'yb-orders-subclient');
            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.content');

            await browser.ybAssertView(
                'просмотр страницы заказов, поиск по субклиенту',
                '.content',
                { hideElements }
            );
        });

        it('заказы, предупреждения о входе под логином без клиента', async function () {
            const { browser } = this;
            await browser.ybSignIn({ login: 'yb-static-balance-5' });
            await browser.ybUrl('user', `orders.xml`);
            await browser.waitForVisible('.yb-notification_type_error');
        });
    });
});
