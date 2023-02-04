const { waitUntilTimeout } = require('../../../helpers');

describe('user', () => {
    describe('consumes-history', () => {
        it('история зачислений, нет зачислений + ссылка в меню', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-consumes' });
            await browser.ybUrl('user', `index.xml`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertLink(
                '.yb-user-nav__item_consumes-history',
                'consumes-history.xml'
            );

            await browser.click('.yb-user-nav__item_consumes-history');
            await browser.waitForVisible('.blc_consumesHistoryFilterForm');

            await browser.ybAssertView('consumes-history, нет зачислений', '.yb-user-content');
        });

        it('КП, закрытый и открытый периоод', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yndx-static-balance-consumes' });
            await browser.ybUrl(
                'user',
                `consumes-history.xml?type=&contract_id=&service_order_id=&operation_dt_from=2021-02-01T00%3A00%3A00&operation_dt_to=&service_group_id=&ps=10`
            );

            await browser.waitForVisible('.blc_consumesHistoryFilterForm');

            // не хайжу данные, тк переналивок не предвидится
            await browser.ybAssertView('consumes-history, есть зачисления', '.yb-user-content');
        });
    });
});
