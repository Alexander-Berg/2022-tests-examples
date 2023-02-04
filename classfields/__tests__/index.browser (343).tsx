import React from 'react';
import { render } from 'jest-puppeteer-react';
import { WithRouterProps } from 'react-router';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Right } from 'types/user';

import { DevchatPageComponent, IDevchatPageProps } from '../';

const mock: IDevchatPageProps = {
    chats: [
        { roomId: '1', itemId: '1', itemCreationTime: '2024-9-24', buyerPhone: '+79991160920', canComplain: true },
        { roomId: '2', itemId: '2', itemCreationTime: '2024-9-24', canComplain: false },
        {
            roomId: '3',
            itemId: '3',
            itemCreationTime: '2024-9-24',
            buyerPhone: '+79991160920',
            canComplain: true,
            complaint: { verdict: 'CHAT_NOT_OK' },
        },
        {
            roomId: '4',
            itemId: '4',
            itemCreationTime: '2024-9-24',
            buyerPhone: '+79991160920',
            canComplain: false,
            complaint: { verdict: 'CHAT_OK' },
        },
        { roomId: '5', itemId: '5', itemCreationTime: '2024-9-24', buyerPhone: '+79991160920', canComplain: true },
    ],
    availableSites: [
        { name: 'Балтика 9', id: 123, location: { subjectFederationRgid: 741965 } },
        { name: 'Балтика 1', id: 321, location: { subjectFederationRgid: 741965 } },
        { name: 'Балтика 6', id: 456, location: { subjectFederationRgid: 741965 } },
    ],
    paging: {
        page: {
            num: 5,
            size: 5,
        },
        total: 120,
        pageCount: 24,
    },
    load: noop,
    isLoading: false,
    complain: () => Promise.reject(),
    clientId: 1,
    getChatMessages: () => Promise.reject(),
    hasComplainRight: true,
    ...({} as WithRouterProps),
} as IDevchatPageProps;

const mockState = {
    page: {
        name: 'partner-devchats',
        params: {
            clientId: '',
            siteId: '123',
            from: '2021-05-28',
            to: '2021-06-03',
        },
    },
    client: {
        id: 1,
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

describe('DevchatPage', () => {
    it('Рендерится с полным набором элементов', async () => {
        await render(
            <AppProvider
                initialState={mockState}
                fakeTimers={{
                    now: new Date('2021-11-12T03:00:00.111Z').getTime(),
                }}
            >
                <DevchatPageComponent {...mock} />
            </AppProvider>,
            { viewport: { width: 1300, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
