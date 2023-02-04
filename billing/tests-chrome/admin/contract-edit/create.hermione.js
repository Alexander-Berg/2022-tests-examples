const {
    assertViewOpts,
    hideElements,
    getAssertViewOptsCol,
    setClient,
    setPerson,
    setDate,
    setManager
} = require('./helpers');
const { basicIgnore } = require('../../../helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', () => {
    describe('contract-edit', () => {
        describe('create', async function () {
            it.skip('создание GENERAL договора', async function () {
                const { browser } = this;

                const [client_id, person_id] = await browser.ybRun(
                    'test_create_client_with_person',
                    ['ur', '0']
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybAssertView(
                    'незаполненная форма выбора типа договора',
                    '.yb-content'
                );

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'GENERAL');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'незаполненный GENERAL договор',
                    '.content',
                    assertViewOpts
                );

                await setClient(browser, client_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setManager(browser, 'Герасимова');
                await setDate(browser, 'dt');
                await browser.selectByValue('select[name="payment-type"]', '2');
                await browser.click('input[id="services-7"]');
                await browser.click('input[id="services-37"]');
                await browser.click('input[id="is-signed"]');

                await browser.ybAssertView(
                    'заполненный договор GENERAL',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор GENERAL',
                    '.content',
                    assertViewOpts
                );
            });

            it('создание GENERAL договора с агентством [smoke]', async function () {
                const { browser } = this;

                const [agency_id, person_id] = await browser.ybRun('test_create_agency_with_ur');

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybAssertView(
                    'незаполненная форма выбора типа договора',
                    '.yb-content'
                );

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'GENERAL');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                assertViewOptsIgnoreServices = {
                    ignoreElements: [...basicIgnore, 'input[id="external-id"]'],
                    hideElements: [...hideElements, '#services-div', '#firm']
                };

                await browser.selectByValue('select[name="commission"]', '4');
                await setClient(browser, agency_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setManager(browser, 'Герасимова');
                await setDate(browser, 'dt');
                await browser.selectByValue('select[name="payment-type"]', '2');
                await browser.click('input[id="services-7"]');
                await browser.click('input[id="services-37"]');
                await browser.click('input[id="is-signed"]');

                await browser.ybAssertView(
                    'заполненный договор GENERAL с агентством',
                    '.content',
                    assertViewOptsIgnoreServices
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор GENERAL с агентством',
                    '.content',
                    assertViewOptsIgnoreServices
                );
            });

            // тест на создание договора, игнорирующий список сервисов
            it('создание GENERAL договора [smoke]', async function () {
                const { browser } = this;

                const [client_id, person_id] = await browser.ybRun(
                    'test_create_client_with_person',
                    ['ur', '0']
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybAssertView(
                    'незаполненная форма выбора типа договора [smoke]',
                    '.yb-content'
                );

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'GENERAL');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                assertViewOptsIgnoreServices = {
                    ignoreElements: [...basicIgnore, 'input[id="external-id"]'],
                    hideElements: [...hideElements, '#services-div', '#firm']
                };

                await browser.ybAssertView(
                    'незаполненный GENERAL договор [smoke]',
                    '.content',
                    assertViewOptsIgnoreServices
                );

                await setClient(browser, client_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setManager(browser, 'Герасимова');
                await setDate(browser, 'dt');
                await browser.selectByValue('select[name="payment-type"]', '2');
                await browser.click('input[id="services-7"]');
                await browser.click('input[id="services-37"]');
                await browser.click('input[id="is-signed"]');

                await browser.ybAssertView(
                    'заполненный договор GENERAL [smoke]',
                    '.content',
                    assertViewOptsIgnoreServices
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор GENERAL [smoke]',
                    '.content',
                    assertViewOptsIgnoreServices
                );
            });

            it('создание SPENDABLE договора', async function () {
                const { browser } = this;

                const [client_id, person_id] = await browser.ybRun(
                    'test_create_client_with_person',
                    ['ur', '1']
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'SPENDABLE');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'незаполненный SPENDABLE договор',
                    '.content',
                    assertViewOpts
                );

                await setClient(browser, client_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setManager(browser, 'Герасимова');
                await setDate(browser, 'dt');
                await browser.selectByValue('select[id="currency"]', '643');
                await browser.click('input[id="services-134"]');
                await browser.click('input[id="is-signed"]');

                await browser.ybAssertView(
                    'заполненный договор SPENDABLE',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор SPENDABLE',
                    '.content',
                    assertViewOpts
                );
            });

            it('создание PARTNERS договора', async function () {
                const { browser } = this;

                const [client_id, person_id] = await browser.ybRun(
                    'test_create_client_with_person',
                    ['ur', '1']
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'PARTNERS');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'незаполненный PARTNERS договор',
                    '.content',
                    assertViewOpts
                );

                await setClient(browser, client_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setDate(browser, 'dt');
                await browser.selectByValue('select[id="currency"]', '643');
                await browser.click('input[id="is-signed"]');

                await browser.ybAssertView(
                    'заполненный договор PARTNERS',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор PARTNERS',
                    '.content',
                    assertViewOpts
                );
            });

            it('создание DISTRIBUTION договора', async function () {
                const { browser } = this;

                const [client_id, person_id, tag_id] = await browser.ybRun(
                    'test_create_client_with_tag_and_person'
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', 'create-contract.xml');
                await browser.waitForVisible('select[name="type"]');
                const mainTabId = await browser.getWindowHandle();

                await browser.ybSetSteps(`Выбирает тип договора и жмет "Выбрать"`);
                await browser.selectByValue('select[name="type"]', 'DISTRIBUTION');
                await browser.click('input[type="submit"]');

                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'незаполненный DISTRIBUTION договор',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип договора Универсальный`);
                await browser.selectByValue('select[id="contract-type"]', '3');

                await setClient(browser, client_id, mainTabId);
                await browser.waitForVisible('div[id="person-id-div"]');
                await setPerson(browser, person_id, mainTabId);

                await browser.ybSetSteps(`Заполняет параметры договора`);
                await setDate(browser, 'dt');
                await setDate(browser, 'service-start-dt');
                await browser.selectByValue('select[id="distribution-tag"]', tag_id);
                await browser.selectByValue('select[id="currency"]', '643');
                await browser.click('input[id="is-signed"]');
                await setManager(browser, 'Ус Александр');

                await browser.ybAssertView(
                    'заполненный договор DISTRIBUTION',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет договор`);
                await browser.click('input[id="button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                await browser.ybAssertView(
                    'сохраненный договор DISTRIBUTION',
                    '.content',
                    assertViewOpts
                );
            });

            it('создание ДС к GENERAL договору', async function () {
                const { browser } = this;

                const [, , contract_id] = await browser.ybRun('test_direct_1_contract', [true]);

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', `contract-edit.xml?contract_id=${contract_id}`);
                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');
                await browser.click('a[id="col-new-create"]');

                await browser.ybAssertView(
                    'просмотр GENERAL договора + незаполненное ДС',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип ДС расторжение договора`);
                await browser.selectByValue('select[id="col-new-collateral-type"]', '90');
                await browser.waitForVisible('div[id="col-new-print-template-div"]');

                await browser.ybSetSteps(`Заполняет параметры ДС`);
                await setDate(browser, 'col-new-dt');
                await browser.setValue('textarea[id="col-new-tickets"]', 'BALANCE-666');
                await browser.setValue('textarea[id="col-new-memo"]', 'вжух');
                await browser.click('input[id="col-new-print-tpl-email"]');
                await browser.click('input[id="col-new-print-tpl-email-manager"]');
                await browser.click('input[id="col-new-is-signed"]');

                await browser.ybAssertView(
                    'заполненное ДС к GENERAL',
                    '.new-collateral',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет ДС`);
                await browser.click('input[id="col-new-button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                let assertViewOptsCol = await getAssertViewOptsCol(browser);

                await browser.ybAssertView(
                    'сохраненное ДС к GENERAL',
                    '.collateral',
                    assertViewOptsCol
                );
            });

            it('создание ДС к DISTRIBUTION договору', async function () {
                const { browser } = this;

                const [, contract_id] = await browser.ybRun(
                    'test_distribution_group_and_child_offer'
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', `contract-edit.xml?contract_id=${contract_id}`);
                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');
                await browser.click('a[id="col-new-create"]');

                await browser.ybAssertView(
                    'просмотр DISTRIBUTION договора + незаполненное ДС',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип ДС прочее`);
                await browser.selectByValue('select[id="col-new-collateral-type"]', '3040');

                await browser.ybSetSteps(`Заполняет параметры ДС`);
                await setDate(browser, 'col-new-dt');
                await browser.setValue('textarea[id="col-new-memo"]', 'вжух');
                await browser.click('input[id="col-new-is-signed"]');

                await browser.ybAssertView(
                    'заполненное ДС к DISTRIBUTION',
                    '.new-collateral',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет ДС`);
                await browser.click('input[id="col-new-button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                let assertViewOptsCol = await getAssertViewOptsCol(browser);

                await browser.ybAssertView(
                    'сохраненное ДС к DISTRIBUTION',
                    '.collateral',
                    assertViewOptsCol
                );
            });

            it('создание ДС к DISTRIBUTION договору [smoke]', async function () {
                const { browser } = this;

                const [, contract_id] = await browser.ybRun(
                    'test_distribution_group_and_child_offer'
                );

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', `contract-edit.xml?contract_id=${contract_id}`);
                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');
                await browser.click('a[id="col-new-create"]');

                let localAsserViewOpts = {
                    ignoreElements: [...basicIgnore, 'input[id="external-id"]'],
                    hideElements: [...hideElements, 'select[id="firm"]']
                };

                await browser.ybAssertView(
                    'просмотр DISTRIBUTION договора + незаполненное ДС [smoke]',
                    '.content',
                    localAsserViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип ДС прочее`);
                await browser.selectByValue('select[id="col-new-collateral-type"]', '3040');

                await browser.ybSetSteps(`Заполняет параметры ДС`);
                await setDate(browser, 'col-new-dt');
                await browser.setValue('textarea[id="col-new-memo"]', 'вжух');
                await browser.click('input[id="col-new-is-signed"]');

                await browser.ybAssertView(
                    'заполненное ДС к DISTRIBUTION [smoke]',
                    '.new-collateral',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет ДС`);
                await browser.click('input[id="col-new-button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                let assertViewOptsCol = await getAssertViewOptsCol(browser);

                await browser.ybAssertView(
                    'сохраненное ДС к DISTRIBUTION [smoke]',
                    '.collateral',
                    assertViewOptsCol
                );
            });

            it('создание ДС к SPENDABLE договору', async function () {
                const { browser } = this;

                const [, contract_id] = await browser.ybRun('test_spendable_dmp_contract');

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', `contract-edit.xml?contract_id=${contract_id}`);
                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');
                await browser.click('a[id="col-new-create"]');

                await browser.ybAssertView(
                    'просмотр SPENDABLE договора + незаполненное ДС',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип ДС прочее`);
                await browser.selectByValue('select[id="col-new-collateral-type"]', '702' + '0');

                await browser.ybSetSteps(`Заполняет параметры ДС`);
                await setDate(browser, 'col-new-dt');
                await browser.setValue('textarea[id="col-new-memo"]', 'вжухвжухжвжух!');
                await browser.click('input[id="col-new-is-signed"]');

                await browser.ybAssertView(
                    'заполненное ДС к SPENDABLE',
                    '.new-collateral',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет ДС`);
                await browser.click('input[id="col-new-button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                let assertViewOptsCol = await getAssertViewOptsCol(browser);

                await browser.ybAssertView(
                    'сохраненное ДС к SPENDABLE',
                    '.collateral',
                    assertViewOptsCol
                );
            });

            it('создание ДС к PARTNERS договору', async function () {
                const { browser } = this;

                const [, contract_id] = await browser.ybRun('test_partners_contract');

                await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
                await browser.ybUrl('admin', `contract-edit.xml?contract_id=${contract_id}`);
                await browser.ybSetSteps(`Ждет открытия страницы редактирования договора`);
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');
                await browser.click('a[id="col-new-create"]');

                await browser.ybAssertView(
                    'просмотр PARTNERS договора + незаполненное ДС',
                    '.content',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Выбирает тип ДС прочее`);
                await browser.selectByValue('select[id="col-new-collateral-type"]', '2040');

                await browser.ybSetSteps(`Заполняет параметры ДС`);
                await setDate(browser, 'col-new-dt');
                await browser.setValue('textarea[id="col-new-memo"]', 'вжухвжухжвжух!');
                await browser.click('input[id="col-new-is-signed"]');

                await browser.ybAssertView(
                    'заполненное ДС к PARTNERS',
                    '.new-collateral',
                    assertViewOpts
                );

                await browser.ybSetSteps(`Сохраняет ДС`);
                await browser.click('input[id="col-new-button-submit"]');
                await browser.waitForVisible('/html/body[1]/table[5]/tbody[1]/tr[1]/td[1]');

                let assertViewOptsCol = await getAssertViewOptsCol(browser);

                await browser.ybAssertView(
                    'сохраненное ДС к PARTNERS',
                    '.collateral',
                    assertViewOptsCol
                );
            });
        });
    });
});
