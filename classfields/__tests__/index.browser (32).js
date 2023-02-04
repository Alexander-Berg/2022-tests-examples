import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientTableErrorsModalContainer from '../container';

import mocks from './storeMock';

const Component = ({ store }) => (
    <AppProviders store={store}>
        <ClientTableErrorsModalContainer />
    </AppProviders>
);

describe('ClientTableErrorsModalContainer', () => {
    it('correct draw pending status', async() => {
        await render(<Component store={mocks.pending} />, { viewport: { width: 800, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw one error', async() => {
        await render(<Component store={mocks.loaded} />, { viewport: { width: 800, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw several errors', async() => {
        await render(<Component store={mocks.loadedSeveral} />, { viewport: { width: 800, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw failed status', async() => {
        await render(<Component store={mocks.failed} />, { viewport: { width: 800, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
