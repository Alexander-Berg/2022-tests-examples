const { waitUntilTimeout } = require('../../../helpers');

describe('user', () => {
    describe('endbuyers-postpay', () => {
        it('КП, проверка ссылки в меню и страницы без лимитов', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const { client_id } = await browser.ybRun(
                'test_agency_with_comm_contract_and_endbuyer',
                { login }
            );

            await browser.ybUrl('user', `index.xml`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertLink(
                '.yb-user-nav__item_endbuyers-postpay',
                'endbuyers-postpay.xml'
            );

            await browser.click('.yb-user-nav__item_endbuyers-postpay');
            await browser.waitForVisible('.blc_clone_subclients_budgets');
            await browser.selectByValue(
                '.blc_subclient_search_form .blc_period_dt',
                '2022-02-01T00:00:00'
            );
            await browser.waitForVisible('.blc_clone_subclients_budgets');

            await browser.ybAssertView(
                'endbuyers-postpay, субклиенты, нет лимитов',
                '.yb-user-content'
            );

            await browser.click('.blc_menu li:nth-child(2) a');
            await browser.waitForVisible('.blc_clone_orders_budgets');

            await browser.ybAssertView(
                'endbuyers-postpay, заказы, нет лимитов',
                '.yb-user-content'
            );
        });
        it('КП, закрытый и открытый периоод', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-balance-7' });
            await browser.ybUrl('user', `endbuyers-postpay.xml`);

            await browser.waitForVisible('.blc_clone_subclients_budgets');
            await browser.selectByValue(
                '.blc_subclient_search_form .blc_period_dt',
                '2021-06-01T00:00:00'
            );
            await browser.waitForVisible('.blc_subclient_search_form__list');

            await browser.ybAssertView(
                'endbuyers-postpay, субклиенты, закрытый период',
                '.yb-user-content'
            );

            await browser.selectByValue(
                '.blc_subclient_search_form .blc_period_dt',
                '2021-07-01T00:00:00'
            );
            await browser.waitForVisible('.blc_subclient_search_form__list');

            await browser.ybAssertView(
                'endbuyers-postpay, субклиенты, открытый период',
                '.yb-user-content'
            );
        });
    });
});
