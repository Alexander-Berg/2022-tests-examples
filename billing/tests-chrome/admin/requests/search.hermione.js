const { setValues, valuesUrl, clearFilter, assertViewOpts, paginationUrl } = require('./helpers');

describe('admin', function () {
    describe('requests', function () {
        it('заполнение фильтра, сброс и формирование URL', async function () {
            const { browser } = this;

            // isReadonly false тк результат не показывается без права IssueInvoices, это ожидаемое поведение
            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'requests.xml');
            await browser.ybWaitForLoad({ waitFilter: true });
            await setValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybAssertView(
                'страница, заполнение значений и поиск',
                '.yb-content',
                assertViewOpts
            );
            await clearFilter(browser);
            await browser.ybAssertView('фильтр, очистка', '.yb-search-filter', assertViewOpts);
        });

        it('пагинация, количество элементов на странице, сортировка и ссылки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertLink('a=371967497', 'paystep.xml?request_id=371967497');
            await browser.ybAssertLink('a=Ledokol Group (5708488)', 'tclient.xml?tcl_id=5708488');

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView(
                'страница, переключение на 2 страницу и сортировка по дате счета',
                '.yb-main',
                {
                    ...assertViewOpts,
                    captureElementFromTop: true,
                    allowViewportOverflow: true,
                    compositeImage: true
                }
            );

            await browser.ybTableChangeSort('№ счета', '.yb-requests-table');
            await browser.ybWaitForLoad({
                waitFilter: true
            });

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView(
                'страница, переключение на отображение по 25 элементов и сортировка по номеру счета',
                '.yb-main',
                {
                    ...assertViewOpts,
                    captureElementFromTop: true,
                    allowViewportOverflow: true,
                    compositeImage: true
                }
            );
        });
    });
});
