import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IPartnerUser, Right } from 'types/user';

import Page404 from '../';

const mockState: Record<string, unknown> = {
    page: {
        params: {},
        queryId: 'asdvasfq3fcaweq4dfqcawx',
    },
    user: {
        isNewLkAvailable: true,
        displayName: 'Vasyan',
        yuid: 'aewce2edqxqXW',
        isAuth: true,
    } as IPartnerUser,
    billingUser: {
        rights: { [Right.CAMPAIGN_BALANCE_CORRECT]: true, [Right.CAMPAIGN_BALANCE_PAY]: true },
    },
};

const mockStateWithClientId: Record<string, unknown> = {
    ...mockState,
    page: {
        name: '404',
        params: {
            clientId: 123,
        },
        queryId: 'asdvasfq3fcaweq4dfqcawx',
    },
};

describe('Page404', () => {
    it('Рендерится без навгации в шапке', async () => {
        await render(
            <AppProvider initialState={mockState}>
                <Page404 />
            </AppProvider>,
            { viewport: { width: 1300, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с навгацией в шапке', async () => {
        await render(
            <AppProvider initialState={mockStateWithClientId}>
                <Page404 />
            </AppProvider>,
            { viewport: { width: 1300, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
