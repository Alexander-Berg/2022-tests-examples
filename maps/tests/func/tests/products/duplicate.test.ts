import cssSelectors from '../../common/css-selectors';
import USERS_BY_ROLE, {UserRole} from '../../common/users';
import {clickAndNavigate, waitForSelector, openPage} from '../../utils/commands';

describe('Duplicate product', () => {
    test('should open duplicate page after click in products list', async () => {
        await openPage({pathname: `/products`, auth: USERS_BY_ROLE.superuser});
        await clickAndNavigate(cssSelectors.products.list.duplicateButton);

        expect(page.url()).toMatch(/products\/\d+\/duplicate/);
    });

    test(`should duplicate product with superuser role`, async () => {
        await openPage({pathname: `/products/113/duplicate`, auth: USERS_BY_ROLE.superuser});
        await clickAndNavigate(cssSelectors.products.create.submit);

        expect(page.url()).toMatch(/products\/\d+\/details/);
        await waitForSelector(cssSelectors.common.dialog.container);
    });

    for (const role of Object.values(UserRole)) {
        if (role !== UserRole.SUPERUSER) {
            test(`should not allow to duplicate product with ${role} role`, async () => {
                await openPage({pathname: `/products/113/duplicate`, auth: USERS_BY_ROLE[role]});
                await waitForSelector(cssSelectors.errors.appError);
            });
        }
    }
});
