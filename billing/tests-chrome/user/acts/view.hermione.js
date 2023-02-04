const { waitUntilTimeout } = require('../../../helpers');
const ignoreElements = ['body #ecopy-captcha-image', '.captcha-image'];
const assert = require('chai').assert;

describe('user', () => {
    describe('acts', () => {
        it('акты, нет актов', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });

            await browser.ybUrl('user', `acts.xml`);
            await browser.waitForVisible('.yb-user-content');
            await browser.ybAssertView('просмотр страницы актов, нет актов', '.yb-user-content', {
                ignoreElements
            });
        });
        it('акты, поиск по плательщику и сервису', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-acts-1' });
            await browser.ybUrl('user', `acts.xml`);

            await browser.waitForVisible('.yb-user-content');
            await browser.ybWaitForInvisible('.xf-advancer');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.selectByValue('select[name="service_group_id"]', '11');
            await browser.selectByValue('select[name="person_id"]', '20328977');
            await browser.click('input[name="ct"]');

            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.yb-user-content');
            await browser.ybWaitForInvisible('.xf-advancer');
            await browser.ybAssertView(
                'просмотр страницы актов, фильтрация по плательщику и сервису',
                '.yb-user-content',
                { ignoreElements }
            );
        });
        it('акты, пагинация', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-acts-1' });
            await browser.ybUrl('user', `acts.xml`);

            await browser.waitForVisible('.yb-user-content');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybWaitForInvisible('.xf-advancer');

            // не хайжу данные, тк переналивки не предвидится
            await browser.ybAssertView(
                'просмотр страницы актов, пагинация, первая страница',
                '.yb-user-content',
                { ignoreElements }
            );

            await browser.click('.pages a');
            await browser.waitForVisible('.yb-user-content');
            await browser.ybWaitForInvisible('.xf-advancer');
            await browser.ybAssertView(
                'просмотр страницы актов, пагинация, вторая страница',
                '.yb-user-content',
                { ignoreElements }
            );
        });
        it('акты, привязка бухлогина', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-acts-1' });

            await browser.ybUrl('user', `acts.xml`);
            await browser.waitForVisible('.yb-user-content');

            await browser.click('#bookkeep-login-link');
            await browser.waitUntil(async function () {
                const style = await browser.getAttribute('.fancybox-wrap', 'style');
                return /opacity: 1/.test(style);
            });
            await browser.waitForVisible('.xf-advanced');
            await browser.ybAssertView(
                'просмотр страницы актов, форма привязки бухлогина',
                '.yb-user-content',
                { ignoreElements }
            );

            const accountantLogin = 'yb-test-accountant-1';
            // на всякий случай убираем связку с клиентом
            await browser.ybRun('test_unlink_client', { login: accountantLogin });
            await browser.ybRun('test_delete_every_accountant_role', { login: accountantLogin });
            await browser.setValue('.fancybox-overlay input[name="login"]', accountantLogin);
            await browser.ybAssertView(
                'просмотр страницы актов, форма привязки бухлогина, введен логин',
                '.yb-user-content',
                { ignoreElements }
            );
            await browser.click('.fancybox-overlay #addLoginButton');
            await browser.ybWaitForInvisible('input[disabled^="disabled"]');
            await browser.ybRun('test_delete_every_accountant_role', { login: accountantLogin });
            const res = await browser.ybRun('test_accountant_status', { login: accountantLogin });
            assert(res != "{'client_id': 1356706577}", 'привязка не удалась');
            await browser.ybRun('test_delete_every_accountant_role', { login: accountantLogin });
        });
        it('акты, заказ оригиналов документов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-acts-1' });
            await browser.ybUrl('user', `acts.xml`);

            await browser.waitForVisible('.yb-user-content');
            await browser.waitForVisible('.xf-advanced');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.selectByValue('select[name="person_id"]', '20328977');
            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.yb-user-content');
            await browser.click('.checkall');
            await browser.waitForVisible(
                '.src-user-pages-acts-components-SendDocumentHardCopy-___style-module__container'
            );
            await browser.ybAssertView(
                'просмотр страницы актов, выбраны все акты',
                '.yb-user-content',
                { ignoreElements }
            );

            await browser.click('#react-container_old button:nth-child(3)');
            await browser.waitForVisible(
                '.src-user-pages-acts-components-SendDocumentHardCopy-___style-module__requestWasSent'
            );
            await browser.ybAssertView(
                'просмотр страницы актов, оригиналы отправлены',
                '.yb-user-content',
                { ignoreElements }
            );
        });

        it('просмотр актов под бухлогином + невозможность привязки бухлогина', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const client_id = 1356706577;
            await browser.ybRun('test_delete_every_accountant_role', { login });
            await browser.ybRun('test_unlink_client', { login });
            await browser.ybRun('test_add_accountant_role', { login, client_id });
            await browser.ybUrl('user', 'acts.xml');
            await browser.waitForVisible('.yb-user-content');
            await browser.ybAssertView(
                'просмотр страницы актов под бухлогином',
                '.yb-user-content',
                {
                    ignoreElements
                }
            );
            await browser.ybWaitForInvisible('#bookkeep-login-link');

            await browser.ybRun('test_delete_every_accountant_role', { login });
        });

        it('акты, предупреждения о входе под логином без клиента', async function () {
            const { browser } = this;
            await browser.ybSignIn({ login: 'yb-static-balance-5' });
            await browser.ybUrl('user', `acts.xml`);
            await browser.waitForVisible('.yb-notification_type_error');
        });
    });
});
