import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';
import { connectAppContext } from 'view/libs/context';

import ClientProfileTable from '../index';

import mocks from './mocks';

const ClientProfileTableWithRouter = ({ data }) => {
    const Component = connectAppContext(
        ({ appContext }) => <ClientProfileTable data={data} router={appContext.router} />
    );

    return <Component />;
};

const Component = ({ data: propsData, store }) => (
    <AppProviders store={store}>
        <ClientProfileTableWithRouter data={propsData} />
    </AppProviders>
);

describe('ClientProfileTable', () => {
    it('correct draw table adAgencyClient', async() => {
        const data = mocks.adAgencyClientMock.client.profile.data;

        await render(
            <Component data={data} store={mocks.adAgencyClientMock} />,
            { viewport: { width: 900, height: 670 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw table normal client', async() => {
        const data = mocks.normalClientMock.client.profile.data;

        await render(
            <Component data={data} store={mocks.normalClientMock} />,
            { viewport: { width: 900, height: 770 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw adAgency', async() => {
        const data = mocks.adAgencyMock.client.profile.data;

        await render(
            <Component data={data} store={mocks.adAgencyMock} />,
            { viewport: { width: 900, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
