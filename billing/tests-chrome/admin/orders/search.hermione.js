const assert = require('chai').assert;

const { basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('orders', function () {
        it('заполнение фильтра', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'orders.xml');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(
                'Заполняет даты, агентство, клиента, номер заказа, включение и сервис'
            );
            await browser.ybSetDatepickerValue('.yb-orders-search__dt-from', '12.06.2018 г.');
            await browser.ybClickOut();
            await browser.ybSetDatepickerValue('.yb-orders-search__dt-to', '14.06.2018 г.');
            await browser.ybClickOut();

            await browser.click('.yb-orders-search__agency .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', '5028445');
            await browser.ybReplaceValue('.yb-clients-search__login', 'Netpeak');
            await browser.ybLcomSelect('.yb-clients-search__agency-select-policy', 'Агентства');
            await browser.ybReplaceValue('.yb-clients-search__phone', '+38 063 80 40 690');
            await browser.ybReplaceValue('.yb-clients-search__email', 'v.krasko@netpeak.net');
            await browser.ybReplaceValue('.yb-clients-search__url', 'http://netpeak.ua/');

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.click('.yb-orders-search__client .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', '42889276');
            await browser.ybReplaceValue('.yb-clients-search__name', 'flagmanamur.ru');
            await browser.ybLcomSelect('.yb-clients-search__agency-select-policy', 'Клиенты');
            await browser.ybReplaceValue('.yb-clients-search__phone', '380948311520');
            await browser.ybReplaceValue('.yb-clients-search__email', 'point.netpeak@gmail.com');

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.ybReplaceValue('.yb-orders-search__service-order-id', '7-35354856');
            await browser.ybLcomSelect('.yb-orders-search__payment-status', 'Включенные');
            await browser.ybLcomSelect('.yb-orders-search__service', 'Директ: Рекламные кампании');

            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Проверяет, что в параметры URL попали новые значения`);
            let url = await browser.getUrl();

            assert.equal(
                url.replace(/^https:\/\/[^\/]+\//, ''),
                'orders.xml?dt_from=2018-06-12T00%3A00%3A00&dt_to=2018-06-14T00%3A00%3A00&agency_id=5028445&client_id=42889276&service_cc=PPC&payment_status=2&service_order_id=7-35354856&pn=1&ps=10&sf=order_dt&so=1',
                'URL не сопадают'
            );

            await browser.ybClickOut();

            await browser.ybAssertView('search заполненный фильтр', 'body', {
                ignoreElements: [...basicIgnore]
            });

            await browser.ybSetSteps(
                'Проверяет, что ссылка в выдаче ведет на страницу заказа, который мы искали'
            );
            const orderUrl = await browser.getAttribute(
                '.yb-orders-table__service-order-id a',
                'href'
            );

            assert.equal(
                orderUrl.replace(/https:\/\/[^\/]+\//, ''),
                `order.xml?service_cc=PPC&service_order_id=35354856`,
                'Неправильная ссылка'
            );

            await browser.ybSetSteps(`Нажимает сбросить на фильтре`);
            await browser.click('.yb-search-filter__button-clear');

            await browser.ybAssertView('search сброс фильтра', 'body', {
                ignoreElements: [...basicIgnore]
            });
        });

        it('переключение страниц, количества элементов и сортировка по дате создания, переход по ссылкам', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl(
                'admin',
                'orders.xml?dt_from=2020-01-01T00%3A00%3A00&dt_to=2020-12-01T00%3A00%3A00&client_id=70860999&payment_status=0&pn=1&ps=10&sf=order_dt&so=1'
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
                ignoreElements: [...basicIgnore]
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
                ignoreElements: [...basicIgnore],
                captureElementFromTop: false
            });

            await browser.ybSetSteps(`Скроллит до кнопки поиска`);
            await browser.scroll('.yb-search-filter__button-search');

            await browser.ybSetSteps(`Переключает на сортировку по дате создания`);
            const ordersTableSort = await browser.ybWaitForExist([
                '.yb-orders-table',
                'th=Дата создания'
            ]);
            await ordersTableSort.click();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertView('search сортировка по дате создания', 'body', {
                ignoreElements: [...basicIgnore]
            });

            await browser.ybAssertLink(
                'a=621-11589756414',
                'order.xml?service_cc=zapravki&service_order_id=11589756414'
            );
            await browser.ybAssertLink(
                'a=pritchenkokarina (70860999)',
                'passports.xml?tcl_id=70860999'
            );
        });
    });
});
