import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/lib/test-helpers';

import SettingsContainer from '../container';

import mocks from './mocks';

const fakeRouter = tab => ({
    params: { settingsTab: tab },
    push: () => {},
    replace: () => {},
    go: () => {},
    goBack: () => {},
    goForward: () => {},
    setRouteLeaveHook: () => {},
    isActive: () => {}
});

const Component = ({ store, tab }) => (
    <AppProvider initialState={store}>
        <WithScrollContextProvider offest={0}>
            <SettingsContainer router={fakeRouter(tab)} />
        </WithScrollContextProvider>
    </AppProvider>
);

describe('SettingsComponent', () => {
    it('natural without tabs', async() => {
        await render(
            <Component store={mocks.natural} />,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('natural without data from vos', async() => {
        await render(
            <Component store={mocks.noVos} />,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('juridical with tabs', async() => {
        await render(
            <Component store={mocks.juridical} />,
            { viewport: { width: 1000, height: 1600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('juridical billing tab', async() => {
        await render(
            <Component store={mocks.juridical} tab="billing" />,
            { viewport: { width: 1000, height: 950 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('juridical billing tab without billing requisites', async() => {
        await render(
            <Component store={mocks.juridicalWithoutBillingRequisites} tab="billing" />,
            { viewport: { width: 1000, height: 750 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ad agency without tabs and without contacts', async() => {
        await render(
            <Component store={mocks.adAgency} />,
            { viewport: { width: 1000, height: 850 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ad agency without billing requisites', async() => {
        await render(
            <Component store={mocks.adAgencyWithoutBillingRequisites} tab="billing" />,
            { viewport: { width: 1000, height: 750 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
