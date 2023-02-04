const common = require('./common');
const tests = require('../tests-generator');
const { Roles, Perms } = require('../../../../helpers/role_perm');
const { openPersonEdit, assertErrorMessage } = require('../helpers');
const { elements } = require('../elements');
const { assert } = require('chai');

describe('admin', () => {
    describe('change-person', () => {
        tests(common);

        describe(`${common.personType}_${common.partner}`, () => {
            describe('создание', () => {
                it('нет права UseAdminPersons', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.UseAdminPersons]
                    });

                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?type=${common.personType}&partner=${common.partner}&client_id=${client_id}`
                    );

                    await browser.ybSetSteps(`Проверяет наличие серверной ошибки`);

                    const errorCodeElem = await browser.$(elements.serverErrorCode);
                    const errorCode = await errorCodeElem.getText();
                    assert.equal(errorCode, 'INVALID_PERSON_TYPE');
                });
            });

            describe('редактирование', () => {
                it('нет права PersonExtEdit', async function () {
                    const { browser } = this;

                    await openPersonEdit(browser, {
                        ...common,
                        signInOpts: {
                            baseRole: Roles.Support,
                            include: [],
                            exclude: [Perms.PersonExtEdit]
                        }
                    });
                    await browser.click(elements.btnSubmit);
                    await assertErrorMessage(browser, {
                        expectedText: 'Недостаточно полномочий для выполнения операции'
                    });
                });
            });
        });
    });
});
