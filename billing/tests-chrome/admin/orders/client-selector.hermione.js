const {
    openClient,
    setClient,
    clearFilter,
    selectByName,
    assertClientSelectorName,
    clearSelectedClient,
    closeModal,
    selectByChooseLink,
    setPaginatableClient,
    setClientById
} = require('./client-selector.helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');
const assertViewOpts = {
    hideElements: ['.yb-clients-table__select-client']
};

describe('admin', function () {
    describe('orders', function () {
        describe('client-selector', function () {
            it('заполнение фильтров на странице + выбор клиента по клику на название + в конце кнопка Сбросить', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'orders.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openClient(browser);

                await setClient(browser);
                await browser.ybFilterDoModalSearch();
                await browser.ybAssertView(
                    'селектор клиента, заполнение и поиск',
                    '.Modal_visible .Modal-Content'
                );

                await clearFilter(browser);
                await browser.ybAssertView(
                    'селектор клиента, сброс',
                    '.Modal_visible .Modal-Content'
                );

                await selectByName(browser);
                await assertClientSelectorName(browser, 'Netpeak');
                await clearSelectedClient(browser);
                await assertClientSelectorName(browser, '');
            });

            it('ссылка Помощь + закрыть окно + выбор клиента по клику на Выбрать', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'orders.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openClient(browser);
                await browser.ybAssertLink(
                    'a=Помощь',
                    'https://doc.yandex-team.ru/Balance/BalanceUG/tasks/HowToFindClientOnChoiseClientPage.html',
                    { isAccurate: true }
                );

                await closeModal(browser);

                await openClient(browser);

                await setClient(browser);
                await browser.ybFilterDoModalSearch();

                await selectByChooseLink(browser);
                await assertClientSelectorName(browser, 'Netpeak');
            });

            it('пагинация, количество элементов на странице', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'orders.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openClient(browser);

                await setPaginatableClient(browser);
                await browser.ybFilterDoModalSearch();

                await browser.ybTableChangePageNumber(2);
                await browser.ybWaitForLoad({ waitModalFilter: true });
                await browser.ybAssertView(
                    'селектор клиента, переключение на страницу 2',
                    '.Modal_visible .Modal-Content',
                    assertViewOpts
                );

                await browser.ybTableChangePageSize(25);
                await browser.ybWaitForLoad({ waitModalFilter: true });
                await browser.ybAssertView(
                    'селектор клиента, переключение на вид по 25 элементов на странице',
                    '.Modal_visible .Modal-Content',
                    assertViewOpts
                );
            });

            it('без права ViewClients', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.ReadOnly,
                    include: [Perms.NewUIEarlyAdopter],
                    exclude: [Perms.ViewClients]
                });
                await browser.ybUrl('admin', 'orders.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openClient(browser);

                await setClientById(browser);
                await browser.ybFilterDoModalSearch();
                await browser.ybAssertView(
                    'селектор клиента, пустой результат без права ViewClients',
                    '.Modal_visible .Modal-Content',
                    assertViewOpts
                );
            });
        });
    });
});
