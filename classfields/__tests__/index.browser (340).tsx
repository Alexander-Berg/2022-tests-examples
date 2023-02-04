import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IPartnerUser, Right } from 'types/user';

import Page403 from '../';

const mock: Record<string, unknown> = {
    page: {
        name: 'partner-devchats',
        params: {
            clientId: 123,
        },
        queryId: '12345687agjnlaeq34rw34FA34CA',
    },
    user: {
        isNewLkAvailable: true,
        displayName: 'Vasyan',
        yuid: 'aewce2edqxqXW',
        isAuth: true,
    } as IPartnerUser,
    passportLink: 'https://passport.yandex.ru/',
    billingUser: {
        rights: { [Right.CAMPAIGN_BALANCE_CORRECT]: true, [Right.CAMPAIGN_BALANCE_PAY]: true },
    },
};

describe('Page403', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={mock}>
                <Page403 />
            </AppProvider>,
            { viewport: { width: 1300, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
