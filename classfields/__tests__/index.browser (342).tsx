import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, infinitePromise } from 'realty-core/view/react/libs/test-helpers';

import { Right } from 'types/user';

import { CallsPageComponent, ICallsPageProps } from '../index';

import { genericCallsList } from './mocks';

const mockProps: ICallsPageProps = {
    calls: [],
    paging: {
        page: {
            num: 5,
            size: 5,
        },
        total: 120,
        pageCount: 24,
    },
    callsLastRequestFilters: {},
    isLoading: false,
    availableCampaigns: [],
    clientId: 123,
    agencyId: 456,
    oldPartnerUrl: 'https://partner.realty.yandex.ru',
    hasComplainRight: true,
    hasSetStatusRight: true,
    load: infinitePromise,
    complain: infinitePromise,
    adminSetStatus: infinitePromise,
};

const mockState = {
    page: {
        name: 'partner-calls',
        params: {
            clientId: '321',
            campaignId: '123',
            from: '2021-05-28',
            to: '2021-06-03',
        },
    },
    client: {
        id: 321,
    },
    agencyClients: [
        { id: 123, client: { name: 'Vasyan' } },
        { id: 321, client: { name: 'Pepsi' } },
    ],
    clientSites: [],
    balance: {
        balance2: {
            current: 6900,
        },
    },
    billingUser: {
        rights: { [Right.CAMPAIGN_BALANCE_CORRECT]: true, [Right.CAMPAIGN_BALANCE_PAY]: true },
    },
};

const Component = (props: Partial<ICallsPageProps>) => (
    <AppProvider
        initialState={mockState}
        fakeTimers={{
            now: new Date('2022-03-09T03:00:00.111Z').getTime(),
        }}
    >
        <CallsPageComponent {...mockProps} {...props} />
    </AppProvider>
);

describe('CallsPage', () => {
    it('Рендерится с пустым списком звонков', async () => {
        await render(<Component paging={{ page: { num: 0, size: 15 }, total: 0, pageCount: 0 }} />, {
            viewport: { width: 1300, height: 600 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со звонками и правами на редактирование статуса', async () => {
        await render(<Component calls={genericCallsList} />, { viewport: { width: 1300, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со звонками без прав на редактирование статуса', async () => {
        await render(<Component calls={genericCallsList} hasSetStatusRight={false} />, {
            viewport: { width: 1300, height: 900 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со звонками без прав на жалобу', async () => {
        await render(<Component calls={genericCallsList} hasComplainRight={false} />, {
            viewport: { width: 1300, height: 900 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится в состоянии загрузки', async () => {
        await render(<Component calls={genericCallsList} isLoading />, {
            viewport: { width: 1300, height: 900 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
