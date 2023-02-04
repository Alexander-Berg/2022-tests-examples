import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import TopSheetModalContainer from '../../container';

const defaultStore = {
    popups: {
        bindMosru: { visible: false }
    },
    vosUserData: {
        extendedUserType: 'OWNER',
        trustedUserInfo: {
            mosRuAvailable: true
        }
    }
};

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <TopSheetModalContainer variant='bindMosru' />
    </AppProvider>
);

describe('BindMosRuModalContainer', () => {
    it('отрисовывается на 1000', async() => {
        await render(<Component store={defaultStore} />, { viewport: { width: 1000, height: 500 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрисовывается на 1400', async() => {
        await render(<Component store={defaultStore} />, { viewport: { width: 1400, height: 500 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
