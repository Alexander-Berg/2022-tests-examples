const {
    setAllValues,
    assertViewOpts,
    valuesUrl,
    clearFilter,
    paginationUrl
} = require('./helpers');
const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('product-catalog', function () {
        it('заполнение фильтра, сброс и формирование URL', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'product-catalog.xml');
            await browser.ybWaitForLoad({ waitFilter: true });
            await setAllValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();
            await browser.ybAssertView('фильтр, заполнение всех значений', '.yb-content', {
                assertViewOpts,
                expandWidth: true
            });

            await browser.ybAssertLink('a=503162', 'product.xml?product_id=503162');
        });

        it('пагинация, количество элементов на странице', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView('страница, переключение на 2 страницу', '.yb-content', {
                ...assertViewOpts,
                captureElementFromTop: true,
                allowViewportOverflow: true,
                expandWidth: true
            });

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertView(
                'страница, переключение на отображение по 25 элементов',
                '.yb-content',
                {
                    ...assertViewOpts,
                    captureElementFromTop: true,
                    allowViewportOverflow: true,
                    expandWidth: true
                }
            );
        });
    });
});
