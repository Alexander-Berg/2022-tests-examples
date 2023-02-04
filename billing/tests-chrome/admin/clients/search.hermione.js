const {
    setValues,
    setAllValues,
    assertViewOpts,
    valuesUrl,
    clearFilter,
    paginationUrl
} = require('./helpers');

describe('admin', function () {
    describe('clients', function () {
        it('заполнение фильтра, сброс и формирование URL [smoke]', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'clients.xml');
            await browser.ybWaitForLoad({ waitFilter: true });
            await setAllValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();
            await browser.ybAssertView(
                'фильтр, заполнение всех значений',
                '.yb-search-filter',
                assertViewOpts
            );

            await clearFilter(browser);
            await setValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView(
                'страница, заполнение значений и поиск',
                'body',
                assertViewOpts
            );

            await browser.ybAssertLink(
                '.yb-clients-table__select-client',
                'tclient.xml?tcl_id=5028445'
            );
            await browser.ybAssertLink(
                'a=Васильева Ирина Владиславовна',
                'invoices.xml?client_id=5028445&manager_code=30010'
            );
            await browser.ybAssertLink('a=v.krasko@netpeak.net', 'mailto:v.krasko@netpeak.net');
        });

        it('пагинация, количество элементов на странице', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView('страница, переключение на 2 страницу', '.yb-main', {
                ...assertViewOpts,
                captureElementFromTop: true,
                allowViewportOverflow: true,
                compositeImage: true
            });

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView(
                'страница, переключение на отображение по 25 элементов',
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
