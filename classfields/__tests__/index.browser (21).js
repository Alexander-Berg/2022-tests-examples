import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientFeedsContainer from '../container';

import storeMock from './storeMock';

const context = {
    router: {
        entries: [ { page: 'clientFeeds', params: { clientId: '1337' } } ]
    }
};

const Component = (
    <AppProviders store={storeMock} context={context}>
        <ClientFeedsContainer />
    </AppProviders>
);

describe('ClientFeeds', () => {
    it('correct draw client feeds', async() => {
        await render(Component, { viewport: { width: 750, height: 800 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
