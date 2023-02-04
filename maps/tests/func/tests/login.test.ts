import cssSelectors from '../common/css-selectors';
import USERS_BY_ROLE, {UserRole} from '../common/users';
import {openPage} from '../utils/commands';

describe('Login', () => {
    for (const role of Object.values(UserRole)) {
        test(`should shown logo after login by ${role}`, async () => {
            await openPage({pathname: `/`, auth: USERS_BY_ROLE[role]});
            await page.waitForSelector(cssSelectors.common.logo);
        });
    }
});
