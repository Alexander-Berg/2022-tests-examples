import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import storeMock from './storeMock';
import clientCampaignsContainerFactory from './../containerFactory';

const context = {
    router: {
        entries: [ { page: 'clientSitesCampaigns', params: { clientId: '1337' } } ]
    }
};
const ClientCampaignsSiteContainer = clientCampaignsContainerFactory('sitesCampaigns');

const Component = (
    <AppProviders store={storeMock} context={context}>
        <ClientCampaignsSiteContainer />
    </AppProviders>
);

describe('ClientCampaigns', () => {
    it('correct draw client campaigns', async() => {
        await render(Component, { viewport: { width: 900, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
