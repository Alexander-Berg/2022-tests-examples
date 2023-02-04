const {
    openPersonCreation,
    openPersonEdit,
    setValues,
    assertMandatoryFields,
    assertAllFields,
    assertPersonPostAddressEditDetails,
    assertEditedDetails,
    directOpenPersonCreation,
    assertDetailsValidation,
    assertPersonPostAddressEditDetailsEdit
} = require('./helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');

module.exports = function ({ partner, personType, details }) {
    describe(`${personType}_${partner}`, () => {
        describe('создание', () => {
            it('только обязательные поля', async function () {
                const { browser } = this;

                await openPersonCreation(browser, { personType, partner });
                await setValues(
                    browser,
                    details.filter(
                        detail => detail.isAdmin !== false && detail.isMandatory === true
                    )
                );
                await assertMandatoryFields(browser);
            });

            it('все поля', async function () {
                const { browser } = this;

                await openPersonCreation(browser, { personType, partner });
                await setValues(
                    browser,
                    details.filter(detail => detail.isAdmin !== false)
                );
                await assertAllFields(browser);
            });

            it('только обязательные поля, нет права PersonPostAddressEdit', async function () {
                const { browser } = this;

                await openPersonCreation(browser, {
                    personType,
                    partner,
                    signInOpts: {
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    }
                });
                await setValues(
                    browser,
                    details.filter(detail => detail.isAdmin !== true && detail.isMandatory === true)
                );
                await assertPersonPostAddressEditDetails(browser);
            });
        });

        describe('редактирование', () => {
            it('без ограничения прав', async function () {
                const { browser } = this;

                await openPersonEdit(browser, { personType, partner });
                await setValues(
                    browser,
                    details.filter(detail => detail.newValue !== undefined),
                    { edit: true }
                );
                await assertEditedDetails(browser);
            });

            it('нет права PersonPostAddressEdit', async function () {
                const { browser } = this;

                await openPersonEdit(browser, {
                    personType,
                    partner,
                    signInOpts: {
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    }
                });
                await assertPersonPostAddressEditDetailsEdit(browser);
            });
        });

        describe('валидация', () => {
            it('обязательные поля', async function () {
                const { browser } = this;

                await directOpenPersonCreation(browser, { personType, partner });
                await assertDetailsValidation(browser);
            });
        });
    });
};
