const { addClientData, editClientData, elements, helpers, options } = require('./common');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', () => {
    describe('addtclient', function () {
        describe('клиент', function () {
            it('только обязательные поля', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);
                await browser.ybAssertView('addtclient.xml - пустая форма', elements.page, options);
                await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
                await helpers.save(browser);
                await browser.ybAssertView(
                    'passports.xml - карточка созданного клиента - только обязательные пол',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
            });

            it('все поля', async function () {
                const { browser } = this;

                const { id, login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await helpers.unlink(browser, id);

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);

                await helpers.createFillAll(browser, { login, isAgency: false });

                await helpers.save(browser);
                await browser.ybAssertView(
                    'passports.xml - карточка созданного клиента - все поля',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
                await helpers.unlink(browser, id);
            });

            it('обязательность полей', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);

                await browser.ybReplaceValue('.yb-edit-client__phone input', addClientData.phone);
                await helpers.trySave(browser);
                await browser.ybAssertView(
                    'addtclient.xml - клиент - не заполнено Название',
                    elements.page,
                    options
                );
                await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
                await browser.ybAssertView(
                    'addtclient.xml - клиент - форма разблокируется после ввода Названия',
                    elements.page,
                    options
                );

                await browser.ybSetLcomCheckboxValue(
                    '.yb-edit-client__isNonResident',
                    addClientData.isNonResident
                );
                await helpers.trySave(browser);
                await browser.ybAssertView(
                    'addtclient.xml - клиент - не заполнены обязательные поля для Нерезидента',
                    elements.page,
                    options
                );
            });

            it('отмена', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await browser.ybUrl('admin', 'invoices.xml');
                await browser.waitForVisible('.yb-nav-item_item-id_1008');
                await browser.click('.yb-nav-item_item-id_1008');
                await browser.waitForVisible('.yb-sub__nav-item*=Добавить');
                await browser.click('.yb-sub__nav-item*=Добавить');
                await browser.waitForVisible(elements.editForm);

                await helpers.cancel(browser);
                await browser.ybWaitForLoad({ waitFilter: true });
            });

            it('нет прав AdditionalFunctions, BillingSupport, ClientFraudStatusEdit', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.BackOffice,
                    include: [Perms.NewUIEarlyAdopter],
                    exclude: []
                });

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);

                await browser.ybAssertView(
                    'addtclient.xml - клиент - пустая форма, нет прав AdditionalFunctions, BillingSupport, ClientFraudStatusEdit',
                    elements.page,
                    options
                );

                await browser.ybSetSteps('Заполняет Название');
                await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
                await helpers.save(browser);
            });
        });

        describe('клиент-агенство', function () {
            it('только обязательные поля', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);
                await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
                await browser.ybSetLcomCheckboxValue('.yb-edit-client__isAgency', true);
                await helpers.save(browser);
                await browser.ybAssertView(
                    'passports.xml - карточка созданного клиента-агентства - только обязательные пол',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
            });

            it('все поля', async function () {
                const { browser } = this;

                const { id, login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await helpers.unlink(browser, id);

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);

                await helpers.createFillAll(browser, { login, isAgency: true });

                await helpers.save(browser);
                await browser.ybAssertView(
                    'passports.xml - карточка созданного клиента-агентства - все поля',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
                await helpers.unlink(browser, id);
            });
        });
    });
});
