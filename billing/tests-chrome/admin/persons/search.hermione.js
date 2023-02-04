const assert = require('chai').assert;

const { basicIgnore } = require('../../../helpers');
const {
    setValues,
    valuesUrl,
    openPerson,
    paginationUrl,
    waitTimeoutForExtensiveQuery,
    hideModalElements,
    hideElements
} = require('./helpers');

describe('admin', function () {
    describe('persons', function () {
        it('корректность заполнения полей на основе заготовленного url', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'persons.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setValues(browser);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();

            await browser.ybAssertView('фильтр, заполнение', '.yb-persons-search', {
                ignoreElements: basicIgnore,
                hideElements
            });

            await browser.ybAssertView('список, отображение', '.yb-persons-table', {
                ignoreElements: basicIgnore,
                allowViewportOverflow: true,
                hideElements
            });

            await browser.ybFilterClear();

            await browser.ybAssertView('фильтр, сброс', '.yb-persons-search', {
                ignoreElements: basicIgnore,
                hideElements
            });

            await browser.ybAssertLink(
                'a.yb-persons-search__help-link',
                'https://doc.yandex-team.ru/Balance/BalanceUG/tasks/Payers-HowToFindPayer.html',
                { isAccurate: true }
            );
            await browser.ybAssertLink(
                'a.yb-persons-table__person-link',
                'subpersons.xml?tcl_id=42807396#person-5687027'
            );

            await openPerson(browser);

            await browser.ybAssertView(
                'информация о плательщике, отображение',
                '.yb-person-modal',
                {
                    ignoreElements: basicIgnore,
                    hideElements: [...hideModalElements, ...hideElements]
                }
            );
        });

        it('переключение страниц, количества элементов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertView(
                'пагинация, переключение номера страницы',
                '.yb-persons-table',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    captureElementFromTop: true,
                    hideElements
                }
            );

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybTableScrollToEnd();

            await browser.ybAssertView(
                'пагинация, переключение размера страницы',
                '.yb-persons-table',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    captureElementFromTop: true,
                    hideElements
                }
            );
        });
        it('Проверка ссылок', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'persons.xml?id=143146&pn=1&ps=10');
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertLink('a=ВИ ИМХО (393872)', 'tclient.xml?tcl_id=393872');
            await browser.ybAssertLink(
                '.yb-persons-table__invoices-link',
                'invoices.xml?person_id=143146'
            );
            await browser.ybAssertLink('.yb-persons-table__acts-link', 'acts.xml?person_id=143146');
            await browser.ybAssertLink(
                '.yb-persons-table__contracts-link',
                'contracts.xml?person_id=143146'
            );
        });
        it('Проверка ссылки на договор', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'persons.xml?id=265196&pn=1&ps=100');
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertLink(
                '.yb-persons-table__contracts-link',
                'partner-contracts.xml?person_id=265196'
            );
        });
    });
});
