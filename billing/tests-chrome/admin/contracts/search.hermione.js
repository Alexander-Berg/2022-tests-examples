const { basicIgnore } = require('../../../helpers');
const {
    setValues,
    valuesUrl,
    paginationUrl,
    changeSort,
    waitTimeoutForExtensiveQuery
} = require('./helpers');

describe('admin', function () {
    describe('contracts', function () {
        it('корректность заполнения полей на основе заготовленного url и проверка сброса; переход по ссылкам [smoke]', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'contracts.xml');
            await browser.ybWaitForLoad({ waitFilter: true });
            await setValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({ waitFilter: true });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();

            await browser.ybAssertView(
                'страница, заполнение фильтра и отображение списка',
                'body',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    expandWidth: true
                }
            );

            await browser.ybFilterClear();

            await browser.ybAssertView('фильтр, сброс', '.yb-contracts-search', {
                ignoreElements: basicIgnore
            });

            await browser.ybAssertLink('a=24576/13', 'contract.xml?contract_id=180298');
            await browser.ybAssertLink('a=Юлмарт (805242)', 'tclient.xml?tcl_id=805242');
            await browser.ybAssertLink('a=Юлмарт (805242)', 'tclient.xml?tcl_id=805242');
        });

        it('Переход на страницу агенства', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });

            await browser.ybUrl(
                'admin',
                'contracts.xml?agency_id=393872&contract_eid=ИВ-1370%2F1207'
            );
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertLink('a=ВИ ИМХО (393872)', 'tclient.xml?tcl_id=393872');
        });

        it('переключение страниц, количества элементов, сортировка', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await changeSort(browser);

            await browser.ybAssertView('список, переключение сортировки', '.yb-contracts-table', {
                ignoreElements: basicIgnore,
                allowViewportOverflow: true,
                captureElementFromTop: true,
                expandWidth: true
            });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertView(
                'список, переключение номера страницы',
                '.yb-contracts-table',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    captureElementFromTop: true,
                    expandWidth: true
                }
            );

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybTableScrollToEnd();

            await browser.ybAssertView(
                'список, переключение размера страницы',
                '.yb-contracts-table',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    captureElementFromTop: true,
                    expandWidth: true
                }
            );
        });
    });
});
