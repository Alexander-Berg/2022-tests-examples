const { basicHide, basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('acts', function () {
        it('заполнение фильтра', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'acts.xml');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(
                'Заполняет акт, счет-фактуру, номер счета, договора, сервис, фирму, даты от и до, валюту, владельца, плательщика и менеджера'
            );
            await browser.ybReplaceValue('.yb-acts-search__external-id', '109173593');
            await browser.ybReplaceValue('.yb-acts-search__factura', '20191130224403');
            await browser.ybReplaceValue('.yb-acts-search__invoice-eid', 'Б-1835232111-1');
            await browser.ybReplaceValue('.yb-acts-search__contract-eid', '33798/15');
            await browser.ybLcomSelect('.yb-acts-search__service', 'Директ: Рекламные кампании');
            await browser.ybLcomSelect('.yb-acts-search__firm', 'ООО «Яндекс»');
            await browser.ybSetDatepickerValue('.yb-acts-search__act-dt-from', '29.11.2019 г.');
            await browser.ybClickOut();
            await browser.ybSetDatepickerValue('.yb-acts-search__act-dt-to', '01.12.2019 г.');
            await browser.ybClickOut();
            await browser.ybLcomSelect('.yb-acts-search__currency-code', 'RUB');

            await browser.click('.yb-acts-search__client .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', '2124001');
            await browser.ybReplaceValue('.yb-clients-search__name', 'Мегагруп');
            await browser.ybLcomSelect('.yb-clients-search__agency-select-policy', 'Агентства');
            await browser.ybReplaceValue('.yb-clients-search__phone', '+7 (812) 448 39 58');
            await browser.ybReplaceValue('.yb-clients-search__email', 'vlmegagroup@gmail.com');
            await browser.ybSetLcomCheckboxValue('.yb-clients-search__is-accurate', true);

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.click('.yb-acts-search__person .Textinput');

            await browser.ybReplaceValue('.yb-persons-search__name', 'Рекмала');
            await browser.ybLcomSelect('.yb-persons-search__person-type', 'Юр. лицо');
            await browser.ybReplaceValue('.yb-persons-search__person-id', '2410718');
            await browser.ybReplaceValue('.yb-persons-search__inn', '7810340569');
            await browser.ybReplaceValue(
                '.yb-persons-search__email',
                'zs.milyausha@gmail.com;vorobyevann@megagroup.ru;anastas.beletskaya@gmail.com'
            );
            await browser.ybReplaceValue('.yb-persons-search__kpp', '781001001');
            await browser.ybLcomSelect('.yb-persons-search__is-partner', 'Только не партнеры');
            await browser.ybSetLcomCheckboxValue('.yb-persons-search__vip-only', false);

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-persons-table__select-person');
            await browser.click('.yb-persons-table__select-person');

            await browser.ybSetLcomCheckboxValue('.yb-search-filter__show-totals', true);

            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertUrl(
                'acts.xml?external_id=109173593&factura=20191130224403&invoice_eid=%D0%91-1835232111-1&contract_eid=33798%2F15&act_dt_from=2019-11-29T00%3A00%3A00&act_dt_to=2019-12-01T00%3A00%3A00&service_id=7&firm_id=1&currency_num_code=643&client_id=2124001&person_id=2410718&ct=1&pn=1&ps=10&sf=act_dt&so=1'
            );

            // Уводим фокус в пустоту, чтобы не падал FF
            await browser.ybClickOut();

            await browser.ybAssertView('search заполненный фильтр', 'body', {
                ignoreElements: basicIgnore,
                hideElements: basicHide
            });

            await browser.ybAssertLink('.yb-acts-table__external-id a', `act.xml?act_id=110296784`);

            await browser.ybSetSteps(`Нажимает сбросить на фильтре`);
            await browser.click('.yb-search-filter__button-clear');

            await browser.ybAssertView('search сброс фильтра', 'body', {
                ignoreElements: basicIgnore
            });
        });

        it('переключение страниц, количества элементов и сортировка по дате акта', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl(
                'admin',
                'acts.xml?act_dt_to=2020-04-01T00%3A00%3A00&client_id=54201478&pn=1&ps=10&sf=act_dt&so=1'
            );

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Переключает на страницу 2`);
            const pageNumberSelector = await browser.ybWaitForExist([
                '.src-common-components-Table-___table-module__page-number-selector',
                'button=2'
            ]);
            await pageNumberSelector.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertView('search переключение страниц', 'body', {
                ignoreElements: basicIgnore
            });

            await browser.ybSetSteps(`Переключает на отображение по 25 строк на странице`);
            const pageSizeSelector = await browser.ybWaitForExist([
                '.src-common-components-Table-___table-module__page-size-selector',
                'button=25'
            ]);
            await pageSizeSelector.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Скроллит до конца таблицы`);
            await browser.scroll(
                '.src-common-components-Table-___table-module__page-size-selector'
            );
            await browser.waitForVisible(
                '.src-common-components-Table-___table-module__page-size-selector'
            );

            await browser.ybAssertView('search переключение количества элементов', 'body', {
                ignoreElements: basicIgnore,
                captureElementFromTop: false
            });

            await browser.ybSetSteps(`Скроллит до кнопки поиска`);
            await browser.scroll('.yb-search-filter__button-search');

            await browser.ybSetSteps(`Переключает на сортировку по дате акта`);
            const actsTableSort = await browser.ybWaitForExist(['.yb-acts-table', 'th=Дата акта']);
            await actsTableSort.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertView('search сортировка по дате акта', 'body', {
                ignoreElements: basicIgnore
            });
        });
    });
});
