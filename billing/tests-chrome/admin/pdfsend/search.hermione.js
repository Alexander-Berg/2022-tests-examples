const { basicHide, basicIgnore } = require('../../../helpers');
const {
    setValuesFull,
    setValuesPagination,
    contractId,
    waitTimeoutForExtensiveQuery,
    tableHide
} = require('./search.helpers');
const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('pdfsend', function () {
        it('корректность заполнения полей и проверка вывода итогов, сброса', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'pdfsend.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setValuesFull(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybClickOut();

            await browser.ybAssertView(
                'страница, заполнение фильтра и отображение таблицы',
                '.yb-content',
                {
                    allowViewportOverflow: true,
                    expandWidth: true
                }
            );

            await browser.ybFilterClear();

            await browser.ybAssertView('фильтр, сброс', '.yb-contracts-for-pdfsend-search', {
                ignoreElements: basicIgnore
            });

            await browser.ybAssertLink('a=177081/19', 'contract.xml?contract_id=606617');
            await browser.ybAssertLink(
                '.yb-edit-contract-link__edit-5',
                'contract-edit.xml?contract_id=606617&collateral_ps=5'
            );
            await browser.ybAssertLink(
                '.yb-edit-contract-link__edit-all',
                'contract-edit.xml?contract_id=606617&collateral_ps=1000'
            );
        });

        it('переключение страниц, количества элементов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'pdfsend.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await setValuesPagination(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertView(
                'список, переключение номера страницы',
                '.yb-contracts-for-pdfsend-table',
                {
                    hideElements: tableHide
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
                '.yb-contracts-for-pdfsend-table',
                {
                    hideElements: tableHide
                }
            );
        });

        it('отправка клиенту и менеджеру', async function () {
            const { browser } = this;

            await browser.ybRun('clear_sent_contract_emails', [contractId]);

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'pdfsend.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await setValuesPagination(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });

            await browser.ybSetSteps(`Выбирает все договоры`);
            await browser.ybSetLcomCheckboxValue('.yb-search-list__list thead', true);
            await browser.ybAssertView('выбраны все договоры', '.yb-contracts-for-pdfsend-table', {
                hideElements: tableHide
            });

            await browser.ybSetSteps(`Нажимает "Отправить выбранное"`);
            await browser.click('.yb-contracts-for-pdfsend-table__send-checked-items button');
            await browser.waitForVisible('.yb-send-contracts-form');
            await browser.ybAssertView('форма отправки, незаполненная', '.yb-send-contracts-form');

            await browser.ybSetSteps(`Выбирает клиента, менеджера и адрес "от кого"`);
            await browser.ybSetLcomCheckboxValue(
                '.yb-send-contracts-form__is-email-to-client',
                true
            );
            await browser.ybSetLcomCheckboxValue(
                '.yb-send-contracts-form__is-email-to-manager',
                true
            );
            await browser.ybLcomSelect(
                '.yb-send-contracts-form__email-from',
                'comission@yandex-team.ru'
            );
            await browser.ybAssertView(
                'форма отправки, клиенту и менеджеру',
                '.yb-send-contracts-form'
            );

            await browser.click('.yb-send-contracts-form > form > div:nth-child(2) button');
            await browser.ybAssertView(
                'форма отправки во время отправки',
                '.yb-send-contracts-form',
                {
                    ignoreElements: [
                        '.yb-send-contracts-form > form > div:nth-child(2) button:nth-child(1)'
                    ]
                }
            );

            await browser.ybWaitForInvisible(
                '.yb-send-contracts-form > form > div:nth-child(2) button:nth-child(2)'
            );
            await browser.ybAssertView(
                'форма отправки после успешной отправки',
                '.yb-send-contracts-form'
            );
        });

        it('отправка на email', async function () {
            const { browser } = this;

            await browser.ybRun('clear_sent_contract_emails', [contractId]);

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'pdfsend.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await setValuesPagination(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });

            await browser.ybSetSteps(`Выбирает первый договор`);
            await browser.ybSetLcomCheckboxValue(
                '.yb-search-list__list tbody tr:nth-child(1)',
                true
            );
            await browser.ybAssertView('выбран первый договор', '.yb-contracts-for-pdfsend-table', {
                hideElements: tableHide
            });

            await browser.ybSetSteps(`Нажимает "Отправить выбранное" и заполняет форму`);
            await browser.click('.yb-contracts-for-pdfsend-table__send-checked-items button');
            await browser.waitForVisible('.yb-send-contracts-form');
            await browser.ybReplaceValue(
                '.yb-send-contracts-form__email-to',
                'test-balance-notify@yandex-team.ru'
            );
            await browser.ybLcomSelect(
                '.yb-send-contracts-form__email-from',
                'comission@yandex-team.ru'
            );
            await browser.ybReplaceValue(
                '.yb-send-contracts-form__email-subject',
                'Заполненная тема',
                'textarea'
            );
            await browser.ybReplaceValue(
                '.yb-send-contracts-form__email-body',
                'Заполненное тело',
                'textarea'
            );
            await browser.ybAssertView(
                'форма отправки на заданный email с темой и телом',
                '.yb-send-contracts-form'
            );

            await browser.click('.yb-send-contracts-form > form > div:nth-child(2) button');
            await browser.ybWaitForInvisible(
                '.yb-send-contracts-form > form > div:nth-child(2) button:nth-child(2)'
            );
            await browser.ybAssertView(
                'форма отправки после успешной отправки на заданный email',
                '.yb-send-contracts-form'
            );
        });
    });
});
