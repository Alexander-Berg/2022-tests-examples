const { waitUntilTimeout } = require('../../../helpers');

const { elements, hideElements } = require('./elements');

describe('user', () => {
    describe('docs', () => {
        describe('invoices', () => {
            it('нет счетов', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                await browser.ybRun('create_client_for_user', [login]);

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.invoices.emptyList);
                await browser.ybAssertView('invoices, нет счетов у клиента', elements.page, {
                    hideElements: [...hideElements]
                });
            });

            it('пагинация (первая страница, вторая страница, пятая страница)', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                await browser.ybRun('link_client_to_user', [login, 1020474]);

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.invoices.table);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);
                await browser.ybAssertView('invoices, пагинация - 1', elements.page, {
                    hideElements: [...hideElements]
                });

                // 2
                await browser.click(elements.pager.page2);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);
                await browser.ybAssertView('invoices, пагинация - 2', elements.page, {
                    hideElements: [...hideElements]
                });

                // ...
                await browser.click(elements.pager.dotts);
                // 5
                await browser.waitForVisible(elements.pager.page5);
                await browser.click(elements.pager.page5);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);
                await browser.ybAssertView('invoices, пагинация - 5', elements.page, {
                    hideElements: [...hideElements]
                });
            });

            it('поиск по сервису и способу оплаты', async function () {
                const { browser } = this;

                await browser.ybSignIn({ login: 'yndx-static-balance-23' });

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.ybAssertView(
                    'invoices, фильтр - по счету, директ - до поиска',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );

                await browser.click(elements.advancedFilterButton);
                await browser.waitForVisible(elements.advancedFilter);
                await browser.ybSetLcomSelectValue(elements.filter.paymentMethod, 'По счету');
                await browser.ybSetLcomSelectValue(elements.filter.serviceId, 'Яндекс.Директ');
                await browser.ybAssertView(
                    'invoices, фильтр - по счету, директ - фильтр',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );

                await browser.click(elements.btnSearch2);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.ybAssertView(
                    'invoices, фильтр - по счету, директ - после поиска',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );
            });

            it('поиск по договору', async function () {
                const { browser } = this;

                await browser.ybSignIn({ login: 'yndx-static-balance-23' });

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.click(elements.advancedFilterButton);
                await browser.waitForVisible(elements.advancedFilter);
                await browser.ybSetLcomSelectValue(
                    elements.filter.contractId,
                    'договор гео постоплата'
                );
                await browser.ybAssertView(
                    'invoices, фильтр - по договору - фильтр',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );

                await browser.click(elements.btnSearch2);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.ybAssertView(
                    'invoices, фильтр - по договору - после поиска',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );
            });

            it.skip('поиск по валюте + итого', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                await browser.ybRun('create_three_currencies_client', [login]);

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);
                await browser.ybAssertView('invoices, по валюте - до поиска', elements.page, {
                    hideElements: [...hideElements]
                });

                await browser.click(elements.advancedFilterButton);
                await browser.waitForVisible(elements.advancedFilter);
                await browser.ybSetLcomSelectValue(elements.filter.currency, 'Евро');
                await browser.ybAssertView('invoices, по валюте - фильтр', elements.page, {
                    hideElements: [...hideElements]
                });

                await browser.click(elements.btnSettings);
                await browser.waitForVisible(elements.tmblShowTotals);
                await browser.click(elements.tmblShowTotals);
                await browser.click(elements.btnSearch1);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);
                await browser.ybAssertView('invoices, по валюте - после поиска', elements.page, {
                    hideElements: [...hideElements]
                });
            });

            it.skip('есть свободные средства', async function () {
                const { browser } = this;

                await browser.ybSignIn({ login: 'yndx-static-balance-23' });

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.click(elements.invoices.unusedFundsButton);
                await browser.waitForVisible(elements.preload);
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.ybAssertView(
                    'invoices, свободные средства - после поиска',
                    elements.page,
                    {
                        hideElements: [...hideElements]
                    }
                );

                await browser.click(elements.advancedFilterButton);
                await browser.waitForVisible(elements.advancedFilter);
                await browser.ybAssertView('invoices, свободные средства - фильтр', elements.page, {
                    hideElements: [...hideElements]
                });
            });

            it('отображение старого ЛС, нет счетов-квитанций', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                await browser.ybRun('link_client_to_user', [login, 62844867]);

                await browser.ybUrl('user', 'docs.xml#/invoices');
                await browser.ybWaitForLoad();
                await browser.waitForVisible(elements.preload, waitUntilTimeout * 2, true);

                await browser.ybAssertView('invoices, отображение старого ЛС', elements.page, {
                    hideElements: [...hideElements]
                });
            });

            it('заполнение фильтра по урлу', async function () {
                const { browser } = this;

                await browser.ybSignIn({});

                await browser.ybSignIn({ login: 'yndx-static-balance-23' });

                await browser.ybUrl(
                    'user',
                    'docs.xml#/invoices?fromDt=&toDt=&paymentMethodId=1001&paymentType=CREDIT&invoiceEid=%D0%91-3835309301-1&dateType=INVOICE&paymentStatus=TURN_OFF&serviceId=37&serviceOrderId=10590600305&personId=20471818&contractId=16480219&currency=RUB&troubleType=UNUSED_FUNDS&sort=&pn=1&showTotals=false'
                );

                await browser.waitForVisible(elements.preload);
                await browser.click(elements.advancedFilterButton);
                await browser.waitForVisible(elements.advancedFilter);
                // подождать пока кнопка прокрасится
                await new Promise(res => setTimeout(res, 100));
                await browser.ybAssertView(
                    'invoices, заполнение фильтра по урлу - фильтр',
                    elements.page,
                    {
                        hideElements: [...hideElements, elements.btnSearch2]
                    }
                );
            });
        });
    });
});
