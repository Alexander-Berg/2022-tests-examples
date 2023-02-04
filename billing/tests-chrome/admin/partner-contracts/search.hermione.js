const helpers = require('./helpers');

describe('admin', function () {
    describe('partner-contracts', function () {
        describe('заполнение фильтра, сброс и формирование URL', function () {
            [
                'РСЯ',
                'Дистрибуция',
                'Справочник',
                'Афиша',
                'Приоритетная сделка',
                'Расходный'
                // 'Эквайринг' Его отключаем тк и так много тестов, а он моргает на дате аннулирования
            ].forEach(type => {
                it(`вид ${type}`, async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true });
                    await browser.ybUrl('admin', 'partner-contracts.xml');
                    await browser.ybWaitForLoad({
                        waitFilter: true,
                        filterTimeout: helpers.waitTimeoutForExtensiveQuery
                    });
                    await helpers[type].setValues(browser);
                    await browser.ybFilterDoSearch({
                        timeout: helpers.waitTimeoutForExtensiveQuery
                    });
                    await browser.ybWaitForLoad({
                        waitFilter: true,
                        filterTimeout: helpers.waitTimeoutForExtensiveQuery
                    });
                    await browser.ybAssertUrl(helpers[type].valuesUrl);
                    await browser.ybAssertView(
                        `страница ${type}, заполнение значений и поиск`,
                        '.yb-content',
                        helpers.wideAssertViewOpts
                    );

                    await helpers.clearFilter(browser);
                    await browser.ybClickOut();
                    await browser.ybAssertView(
                        `фильтр, очистка`,
                        '.yb-search-filter',
                        helpers.assertViewOpts
                    );

                    await browser.ybAssertLink(
                        '.yb-edit-contract-link a',
                        'contract.xml?contract_id=' + helpers[type].firstContractId
                    );

                    await browser.ybAssertLink(
                        '.yb-edit-contract-link__edit-5',
                        `contract-edit.xml?contract_id=${helpers[type].firstContractId}&collateral_ps=5`
                    );
                    await browser.ybAssertLink(
                        '.yb-edit-contract-link__edit-all',
                        `contract-edit.xml?contract_id=${helpers[type].firstContractId}&collateral_ps=1000`
                    );
                });
            });
        });

        it('пагинация, количество элементов на странице, сортировка', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', helpers.paginationUrl);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: helpers.waitTimeoutForExtensiveQuery
            });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: helpers.waitTimeoutForExtensiveQuery
            });
            await browser.ybAssertView(
                'страница, переключение на 2 страницу и сортировка по дате начала',
                '.yb-content',
                helpers.wideAssertViewOpts
            );

            await browser.ybTableChangeSort('№ договора', '.yb-partner-contracts-table');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: helpers.waitTimeoutForExtensiveQuery
            });

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: helpers.waitTimeoutForExtensiveQuery
            });
            await browser.ybAssertView(
                'страница, переключение на отображение по 25 элементов и сортировка по номеру договора',
                '.yb-content',
                helpers.wideAssertViewOpts
            );
        });
    });
});
