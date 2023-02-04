const { partner, personType, details } = require('./common');
const {
    openPersonEdit,
    setValues,
    assertEditedDetails,
    assertPersonPostAddressEditDetailsEdit
} = require('../helpers');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('редактирование', () => {
                it('редактирование', async function () {
                    const { browser } = this;

                    await openPersonEdit(browser, { personType, partner });
                    await setValues(
                        browser,
                        details.filter(detail => typeof detail.newValue !== 'undefined'),
                        { edit: true }
                    );
                    await assertEditedDetails(browser);
                });

                it('редактирование, нет права PersonPostAddressEdit', async function () {
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
        });
    });
});
