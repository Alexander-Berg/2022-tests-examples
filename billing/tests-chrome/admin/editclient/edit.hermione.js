const { addClientData, editClientData, elements, helpers, options } = require('./common');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', () => {
    describe('editclient', () => {
        describe('клиент', () => {
            it('фрод', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                await browser.ybUrl('admin', 'addtclient.xml');
                await browser.waitForVisible(elements.editForm);
                await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
                await helpers.fillFraud(browser, true, addClientData.fraudType);
                await helpers.save(browser);

                await browser.ybSetSteps('Кликает на ссылку Редактировать клиента');
                await browser.click(elements.editLink);
                await browser.waitForVisible(elements.editForm);
                await browser.ybReplaceValue('.yb-edit-client__name input', editClientData.name);
                await helpers.fillFraud(
                    browser,
                    false,
                    editClientData.fraudType,
                    editClientData.fraudText
                );
                await browser.ybAssertView(
                    'editclinet.xml - изменения фрода на Другой',
                    elements.page,
                    options
                );
                await helpers.save(browser);

                await browser.ybAssertView(
                    'passports.xml - карточка отредактированного клиента с фродом',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
            });

            it('отмена', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                await browser.ybUrl('admin', `passports.xml?tcl_id=${client_id}`);
                await browser.waitForVisible(elements.clientComponent);
                await browser.click(elements.editLink);
                await browser.waitForVisible(elements.editForm);

                await helpers.cancel(browser, elements.clientComponent);
            });

            it('нет прав AdditionalFunctions, BillingSupport, ClientFraudStatusEdit', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.BackOffice,
                    include: [Perms.NewUIEarlyAdopter],
                    exclude: []
                });

                const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                await browser.ybUrl('admin', `editclient.xml?tcl_id=${client_id}`);
                await browser.waitForVisible(elements.editForm);

                await browser.ybSetSteps('Изменяет Название');
                await browser.ybReplaceValue('.yb-edit-client__name input', editClientData.name);

                await browser.ybAssertView(
                    'editclient.xml - клиент - пустая форма, нет прав AdditionalFunctions, BillingSupport, ClientFraudStatusEdit',
                    elements.page,
                    options
                );

                await helpers.save(browser);
            });

            it('овердрафт, оферта', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    isAdmin: true,
                    isReadonly: false
                });

                const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                await browser.ybUrl('admin', `editclient.xml?tcl_id=${client_id}`);
                await browser.waitForVisible(elements.editForm);

                await browser.ybSetSteps('Проставляет Не должен получать овердрафт');
                await browser.ybSetLcomCheckboxValue('.yb-edit-client__denyOverdraft', true);
                await browser.waitForVisible('.yb-messages__confirm-modal');
                await browser.click('button.yb-messages__accept');
                await browser.ybSetSteps('Выставление счетов по оферте');
                await browser.ybSetLcomCheckboxValue(
                    '.yb-edit-client__forceContractlessInvoice',
                    true
                );
                await browser.waitForVisible('.yb-messages__confirm-modal');
                await browser.click('button.yb-messages__accept');

                await helpers.save(browser);
                await browser.ybAssertView(
                    'passports.xml - отредактированного клиента - проставлены овердрафт, оферта',
                    elements.clientComponent,
                    {
                        invisibleElements: elements.clientId
                    }
                );
            });
        });
    });
});
