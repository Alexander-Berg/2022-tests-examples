const { valuesUrl, waitTimeoutForExtensiveQuery, waitForEnabled } = require('./helpers');
const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('print-form-rules', function () {
        it('поиск', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', valuesUrl);

            await waitForEnabled(browser);
            await browser.ybWaitForLoad({
                waitFilter: true
            });

            await browser.ybAssertView(
                'search заполненный фильтр и результаты поиска',
                '.yb-content'
            );

            await browser.ybSetSteps('Проверяет ссылку на ПФ');
            await browser.ybAssertLink(
                'a=__automatic_rtrunk_tplno1428_pfno1432',
                'print-form-rule.xml?rule_id=__automatic_rtrunk_tplno1428_pfno1432'
            );

            await browser.ybSetSteps('Меняет Вид договора');
            await browser.ybLcomSelect('.yb-print-form-rules-search__contract-type', 'Расходный');
            await waitForEnabled(browser);
            await browser.click('.yb-search-filter__button-search');
            await browser.ybWaitForLoad({
                waitFilter: true
            });

            await browser.ybAssertView(
                'search измененный фильтр и результаты поиска',
                '.yb-content'
            );

            await browser.ybSetSteps(`Нажимает сбросить на фильтре`);
            await browser.click('.yb-search-filter__button-clear');

            await waitForEnabled(browser);
            await browser.ybWaitForLoad({
                waitFilter: true
            });

            await browser.ybAssertView('search сброс фильтра', '.yb-search-filter');
        });
    });
});
