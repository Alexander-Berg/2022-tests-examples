const {
    openPerson,
    setPerson,
    clearFilter,
    selectByName,
    assertPersonSelectorName,
    clearSelectedPerson,
    closeModal,
    selectByChooseLink,
    setPaginatablePerson,
    waitTimeoutForExtensiveQuery,
    setPersonById,
    personSelectorContentSelector,
    personsTableSelector,
    hideElements
} = require('./person-selector.helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('invoices', function () {
        describe('person-selector', function () {
            it('заполнение фильтров на странице, выбор плательщика по клику на название, в конце кнопка Сбросить', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'invoices.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openPerson(browser);

                await setPerson(browser);
                await browser.ybFilterDoModalSearch();
                await browser.ybAssertView(
                    'селектор плательщика, заполнение и поиск',
                    personSelectorContentSelector
                );

                await clearFilter(browser);
                await browser.ybAssertView(
                    'селектор плательщика, сброс',
                    personSelectorContentSelector
                );

                await selectByName(browser);
                await assertPersonSelectorName(browser, 'КУМИТ');
                await clearSelectedPerson(browser);
                await assertPersonSelectorName(browser, '');
            });

            it('ссылка Помощь + закрыть окно + выбор плательщика по клику на Выбрать + ссылки на клиента, счета, акты, договоры', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'invoices.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openPerson(browser);
                await browser.ybAssertLink(
                    'a=Помощь',
                    'https://doc.yandex-team.ru/Balance/BalanceUG/tasks/Payers-HowToChoicePayer.xml',
                    { isAccurate: true }
                );

                await closeModal(browser);

                await openPerson(browser);

                await setPerson(browser);
                await browser.ybFilterDoModalSearch();

                await browser.ybAssertLink(
                    '.yb-persons-table__client a',
                    'tclient.xml?tcl_id=42807396'
                );
                await browser.ybAssertLink(
                    '.yb-persons-table__invoices-link',
                    'invoices.xml?person_id=5687027'
                );
                await browser.ybAssertLink(
                    '.yb-persons-table__acts-link',
                    'acts.xml?person_id=5687027'
                );
                await browser.ybAssertLink(
                    '.yb-persons-table__contracts-link',
                    'contracts.xml?person_id=5687027'
                );

                await selectByChooseLink(browser);
                await assertPersonSelectorName(browser, 'КУМИТ');
            });

            it('пагинация, количество элементов на странице', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true });
                await browser.ybUrl('admin', 'invoices.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openPerson(browser);

                await setPaginatablePerson(browser);
                await browser.ybFilterDoModalSearch({
                    filterTimeout: waitTimeoutForExtensiveQuery
                });

                await browser.ybTableChangePageNumber(2);
                await browser.ybWaitForLoad({
                    waitModalFilter: true,
                    filterTimeout: waitTimeoutForExtensiveQuery
                });
                await browser.ybAssertView(
                    'селектор плательщика, переключение на страницу 2',
                    personSelectorContentSelector,
                    { hideElements }
                );

                await browser.ybTableChangePageSize(25);
                await browser.ybWaitForLoad({
                    waitModalFilter: true,
                    filterTimeout: waitTimeoutForExtensiveQuery
                });
                await browser.scroll(personsTableSelector);
                // Если делать скрин целиком, из-за особенностей модалки склеивается криво и периодически падает
                await browser.ybAssertView(
                    'таблица плательщиков, переключение на вид по 25 элементов на странице, часть влезающая в модалку',
                    personsTableSelector,
                    { hideElements }
                );
            });

            it('без права ViewPersons', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.ReadOnly,
                    include: [Perms.NewUIEarlyAdopter],
                    exclude: [Perms.ViewPersons]
                });
                await browser.ybUrl('admin', 'invoices.xml');
                await browser.ybWaitForLoad({ waitFilter: true });

                await openPerson(browser);

                await setPersonById(browser);
                await browser.ybFilterDoModalSearch();
                await browser.ybAssertView(
                    'селектор плательщика, пустой результат без права ViewPersons',
                    personSelectorContentSelector
                );
            });
        });
    });
});
