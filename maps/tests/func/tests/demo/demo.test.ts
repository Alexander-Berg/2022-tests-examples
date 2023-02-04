import {openPage} from'../../utils/commands';
import cssSelectors from '../../common/css-selectors';

describe('Initial test', () => {
    test('should show body', async () => {
        await openPage({useAuth: true});
        return page.waitForSelector(cssSelectors.userAccount.userPic);
    });
});
