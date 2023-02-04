const assert = require('chai').assert;

const { basicIgnore } = require('../../../helpers');
const { getInvoiceDate } = require('./helpers');

describe('admin', function () {
    describe('deferpays', function () {
        it('заполнение фильтра', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'deferpays.xml');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(
                'Заполняет агентство, оплату, сервис, договор, номер заказа и даты'
            );
            await browser.ybLcomSelect('.yb-deferpays-search__payment-status', 'Счет оплачен');
            await browser.ybLcomSelect(
                '.yb-deferpays-search__service',
                'Директ: Рекламные кампании'
            );
            await browser.ybReplaceValue('.yb-deferpays-search__order-id', '7-43203039');
            await browser.ybSetDatepickerValue('.yb-deferpays-search__date-from', '01.09.2019 г.');
            await browser.ybClickOut();
            await browser.ybSetDatepickerValue('.yb-deferpays-search__date-to', '30.09.2019 г.');
            await browser.ybClickOut();

            await browser.click('.yb-deferpays-search__agency .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', '410212');
            await browser.ybReplaceValue('.yb-clients-search__name', 'Форсайт');

            await browser.ybLcomSelect('.yb-clients-search__agency-select-policy', 'Агентства');
            await browser.ybReplaceValue(
                '.yb-clients-search__phone',
                '8 (800) 301-01-61, +7 (863) 333-01-21'
            );
            await browser.ybReplaceValue('.yb-clients-search__email', 'baraev@forsite.ru');
            await browser.ybReplaceValue('.yb-clients-search__url', 'www.forsite.ru');
            await browser.ybSetLcomCheckboxValue('.yb-clients-search__is-accurate', true);

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.ybWaitForInvisible('.is-fetching');
            await browser.ybLcomSelect('.yb-deferpays-search__contract', '34054/15');

            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Проверяет, что в параметры URL попали новые значения`);

            const url = await browser.getUrl();

            assert.equal(
                url.replace(/^https:\/\/[^\/]+\//, ''),
                'deferpays.xml?client_id=410212&payment_status=6&service_cc=PPC&contract_id=204942&service_order_id=7-43203039&issue_dt_from=2019-09-01T00%3A00%3A00&issue_dt_to=2019-09-30T00%3A00%3A00&pn=1&ps=10&sf=issue_dt&so=0',
                'URL не сопадают'
            );

            await browser.ybClickOut();

            await browser.ybSetSteps(
                `Проверяет скриншотом "search заполненный фильтр", что все поля вводятся`
            );
            await browser.ybAssertView('search заполненный фильтр', '.yb-deferpays-search', {
                ignoreElements: [...basicIgnore]
            });

            await browser.ybSetSteps(
                `Проверяет скриншотом "search заполненный список", что данные с сервера отображаются`
            );
            await browser.ybAssertView('search заполненный фильтр список', '.yb-deferpays-table', {
                ignoreElements: [...basicIgnore],
                allowViewportOverflow: true,
                expandWidth: true
            });

            await browser.ybSetSteps(
                `Проверяет правильность ссылок на страницы клиента, заказа и счета на погашение`
            );
            await browser.ybAssertLink(
                'a=7-43203039',
                'order.xml?service_cc=PPC&service_order_id=43203039'
            );
            await browser.ybAssertLink('a=Банкет Холл (59703090)', 'tclient.xml?tcl_id=59703090');
            await browser.ybAssertLink('a=Б-1891597659-1', 'invoice.xml?invoice_id=101927730');

            await browser.ybSetSteps(
                'Проверяет, что ссылка в выдаче ведет правильную на страницу счета'
            );
            const invoiceUrl = await browser.getAttribute('.yb-deferpays-table__invoice a', 'href');

            assert.equal(
                invoiceUrl.replace(/^https:\/\/[^\/]+\//, ''),
                `invoice.xml?invoice_id=100391596`,
                'Неправильная ссылка'
            );

            await browser.ybSetSteps(`Нажимает сбросить на фильтре`);
            await browser.click('.yb-search-filter__button-clear');

            await browser.ybSetSteps(
                `Проверяет скриншотом "search сброс фильтра", что поля пустые`
            );
            await browser.ybAssertView('search сброс фильтра', '.yb-deferpays-search', {
                ignoreElements: [...basicIgnore]
            });
        });

        it('переключение страниц, количества элементов и сортировка по дате создания', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl(
                'admin',
                'deferpays.xml?client_id=47019438&payment_status=0&contract_id=0&issue_dt_to=2020-03-31T00%3A00%3A00&pn=1&ps=10&sf=issue_dt&so=0'
            );

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Переключает на сортировку по дате снятия средств`);
            const deferpaysTableSort = await browser.ybWaitForExist([
                '.yb-deferpays-table',
                'th=Дата снятия средств'
            ]);
            await deferpaysTableSort.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(
                `Проверяет скриншотом "search сортировка по дате снятия средств"`
            );
            await browser.ybAssertView(
                'search сортировка по дате снятия средств',
                '.yb-deferpays-table',
                {
                    ignoreElements: [...basicIgnore],
                    allowViewportOverflow: true,
                    captureElementFromTop: true,
                    expandWidth: true
                }
            );

            await browser.ybSetSteps(`Переключает на страницу 2`);
            const pageNumberSelector = await browser.ybWaitForExist([
                '.src-common-components-Table-___table-module__page-number-selector',
                'button=2'
            ]);
            await pageNumberSelector.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Проверяет скриншотом "search переключение страниц"`);
            await browser.ybAssertView('search переключение страниц', '.yb-deferpays-table', {
                ignoreElements: [...basicIgnore],
                allowViewportOverflow: true,
                captureElementFromTop: true,
                expandWidth: true
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

            await browser.ybSetSteps(
                `Проверяет скриншотом "search переключение количества элементов"`
            );
            await browser.ybAssertView(
                'search переключение количества элементов',
                '.yb-deferpays-table',
                {
                    ignoreElements: [...basicIgnore],
                    allowViewportOverflow: true,
                    captureElementFromTop: true
                }
            );
        });

        it('действия с заказами', async function () {
            const { browser } = this;

            await browser.ybSetSteps(`Получает id клиента`);
            const [clientId] = await browser.ybRun('test_fictive_3_orders');

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'deferpays.xml');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps('Заполняет клиента');
            await browser.click('.yb-deferpays-search__agency .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', String(clientId));

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.ybWaitForLoad({});

            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps('Выбирает все счета');
            await browser.click('.yb-deferpays-table__checkbox-row');

            await browser.ybSetSteps('Выставляет счет на погашение на 5 дней позже текущей даты');
            await browser.ybLcomSelect(
                '.yb-deferpays-action__action',
                'Выставить счет на погашение'
            );
            await browser.ybSetDatepickerValue(
                '.yb-deferpays-action__datepicker',
                getInvoiceDate()
            );
            await browser.ybClickOut();

            await browser.click('.yb-deferpays-action__button-action');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps('Выбирает все счета');
            await browser.click('.yb-deferpays-table__checkbox-row');

            await browser.ybSetSteps('Подтверждает счета на погашение');
            await browser.ybLcomSelect(
                '.yb-deferpays-action__action',
                'Подтвердить предварительные счета на погашение'
            );

            await browser.click('.yb-deferpays-action__button-action');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps('Проверяет, что чекбокс выбора счета отсутствует');
            let isActionVisible = await browser.isVisible('.yb-deferpays-table__checkbox-row');
            assert.equal(isActionVisible, false, 'Чекбокс выбора счета должен отсутствовать');
        });
    });
});
