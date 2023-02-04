import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientProfileBalances from '../index';

const Component = ({ balances }) => (
    <AppProviders>
        <ClientProfileBalances balances={balances} />
    </AppProviders>
);

describe('ClientProfileBalances', () => {
    it('correct draw without balances', async() => {
        await render(<Component balances={[]} />, { viewport: { width: 250, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with balances', async() => {
        const balances = [
            {
                sum: 1337,
                description: 'Коммерческая недвижимость'
            },
            {
                sum: 1338,
                description: 'Коммерческая недвижимость2',
                lastIncomeDate: '2019-12-03T22:09:44.241Z'
            },
            {
                sum: 1339,
                description: 'Коммерческая недвижимость3',
                daysBeforeEndOfFunds: 2
            },
            {
                sum: 1340,
                description: 'Коммерческая недвижимость4',
                daysBeforeEndOfFunds: 18
            },
            {
                sum: 1341,
                description: 'Коммерческая недвижимость5',
                daysBeforeEndOfFunds: 0
            }
        ];

        await render(<Component balances={balances} />, { viewport: { width: 280, height: 550 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
