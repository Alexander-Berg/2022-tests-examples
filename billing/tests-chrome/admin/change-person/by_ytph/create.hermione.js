const { partner, personType, details } = require('./common');
const {
    openPersonCreation,
    setValues,
    assertMandatoryFields,
    assertAllFields,
    assertPersonPostAddressEditDetails
} = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
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
                        details.filter(
                            detail => detail.isAdmin !== true && detail.isMandatory === true
                        )
                    );
                    await assertPersonPostAddressEditDetails(browser);
                });
            });
        });
    });
});
