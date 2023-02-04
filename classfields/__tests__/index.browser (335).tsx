import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IAgencyClientListEntry, Right } from 'types/user';

import { SubheaderComponent } from '../';
import { ISubheaderProps } from '../types';

const mock: ISubheaderProps = {
    clients: ([
        { id: 123, client: { name: 'Vasyan' } },
        { id: 321, client: { name: 'Pepsi' } },
    ] as unknown) as IAgencyClientListEntry[],
    partnerUrl: '',
    link: () => '',
    currentClient: {
        id: 321,
        name: 'Саня Агент',
    },
    page: 'partner-devchats',
    shouldRenderBudgetControls: true,
};

const mockState = {
    client: {
        id: 321,
    },
    billingUser: {
        rights: { [Right.CAMPAIGN_BALANCE_CORRECT]: true, [Right.CAMPAIGN_BALANCE_PAY]: true },
    },
};

const Component = (props: ISubheaderProps) => (
    <AppProvider initialState={mockState}>
        <SubheaderComponent {...props} />
    </AppProvider>
);

describe('Subheader', () => {
    it('Рендерится', async () => {
        await render(<Component {...mock} />, { viewport: { width: 1300, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без бюджета', async () => {
        const props = { ...mock, shouldRenderBudgetControls: false };

        await render(<Component {...props} />, { viewport: { width: 1300, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с одним клиентом (когда не из под агентства)', async () => {
        const props = { ...mock, clients: [] };

        await render(<Component {...props} />, { viewport: { width: 1300, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
