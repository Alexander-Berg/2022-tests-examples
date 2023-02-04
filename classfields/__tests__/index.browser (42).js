import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import TopSheetModalContainer from '../../container';

const defaultStore = {
    popups: {
        profilePromo: { visible: false }
    },
    vosUserData: {
        extendedUserType: 'AGENT'
    },
    user: {
        isJuridical: true
    }
};

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <TopSheetModalContainer variant='profilePromo' />
    </AppProvider>
);

describe('ProfilePromoBanner', () => {
    it('отрисовывается на 1000', async() => {
        await render(<Component store={defaultStore} />, { viewport: { width: 1000, height: 500 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрисовывается на 1400', async() => {
        await render(<Component store={defaultStore} />, { viewport: { width: 1400, height: 500 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
