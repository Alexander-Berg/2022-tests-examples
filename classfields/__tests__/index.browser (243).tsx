import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IVillageSnippetType } from 'realty-core/types/villageSnippet';

import { MortgageVillages } from '..';

import { villages } from './mocks';

const Component = ({ items = villages }: { items?: IVillageSnippetType[] }) => (
    <AppProvider>
        <MortgageVillages items={items} searchQuery={{}} />
    </AppProvider>
);

describe('MortgageVillages', () => {
    it('Рисует карусель с КП', async () => {
        await render(<Component />, { viewport: { width: 360, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель с КП (горизонтально)', async () => {
        await render(<Component />, { viewport: { width: 700, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
