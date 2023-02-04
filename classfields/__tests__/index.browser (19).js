import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientAdAgencyClientsContainer from '../../ClientAdAgencyClients/container';

import defaultStoreMock from './storeMock';

const context = {
    router: {
        entries: [ { page: 'clientAdAgencyClients', params: { clientId: '1337' } } ]
    }
};

const Component = ({ store = defaultStoreMock }) => (
    <AppProviders store={store} context={context}>
        <ClientAdAgencyClientsContainer />
    </AppProviders>
);

describe('ClientAdAgencyClients', () => {
    it('correct draw ad agency client clients', async() => {
        await render(<Component />, { viewport: { width: 1800, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw ad agency client clients without manager column without permissions', async() => {
        const storeMock = {
            ...defaultStoreMock,
            user: {
                ...defaultStoreMock.user,
                permissions: []
            }
        };

        await render(<Component store={storeMock} />, { viewport: { width: 1700, height: 700 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
