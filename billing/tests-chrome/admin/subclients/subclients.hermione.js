const { elements } = require('./common');

describe('admin', () => {
    describe('subclients', () => {
        it('нет субклиентов', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('create_empty_agency');

            await browser.ybUrl('admin', `subclients.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.searchEmpty);
            await browser.ybAssertView('нет субклиентов', elements.searchEmpty);
        });

        it('несколько субклиентов, в т.ч. один без заказа (без пагинации), поиск, кнопка "сбросить", формирование URL', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { agency_id, clients_with_orders } = await browser.ybRun('multiple_subclients');

            await browser.ybUrl('admin', `subclients.xml?tcl_id=${agency_id}`);

            await browser.waitForVisible(elements.searchList);
            // TODO: раскомментировать, когда результат сортировки будет стабилен
            // https://st.yandex-team.ru/BALANCE-33973#5f281fc68d159675fc74ac58
            // await browser.ybAssertView('все субклиенты', elements.searchList);

            await browser.ybSetSteps(
                'Заполняет фильтр: "Id, имя или логин клиента"="' + clients_with_orders[0] + '"'
            );
            await browser.ybReplaceValue(elements.searchText, clients_with_orders[0]);
            // Поиск по имени очень долгий. При поиске по id фильтр снимать нельзя
            // await browser.ybAssertView('заполненный фильтр', elements.searchFilter);
            await browser.ybFilterDoSearch();
            await browser.ybAssertView('результат поиска', elements.searchList);

            // await browser.ybAssertUrl(`subclients.xml?tcl_id=${agency_id}&search_text=${encodeURIComponent('физик')}&pn=1&ps=10`);
            await browser.ybAssertUrl(
                `subclients.xml?tcl_id=${agency_id}&search_text=${clients_with_orders[0]}&pn=1&ps=10`
            );

            await browser.click(elements.btnClear);
            await browser.ybAssertView('фильтр после сброса значений', elements.searchFilter);
        });

        it('пагинация, количество элементов на странице ', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { agency_id } = await browser.ybRun('client_with_many_subclients_for_pagination');

            await browser.ybUrl('admin', `subclients.xml?tcl_id=${agency_id}`);

            await browser.waitForVisible(elements.searchList);
            // TODO: раскомментировать, когда результат сортировки будет стабилен
            // https://st.yandex-team.ru/BALANCE-33973#5f281fc68d159675fc74ac58
            // await browser.ybAssertView('страница 1', elements.searchList);
            await browser.click(elements.pageTwo);
            await browser.ybWaitForInvisible('.yb-search-filter__button-search_progress');
            // TODO: раскомментировать, когда результат сортировки будет стабилен
            // https://st.yandex-team.ru/BALANCE-33973#5f281fc68d159675fc74ac58
            // await browser.ybAssertView('страница 2', elements.searchList);
            await browser.ybAssertUrl(`subclients.xml?tcl_id=${agency_id}&pn=2&ps=10`);
        });
    });
});
