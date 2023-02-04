import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { MortgageSites } from '../';

import { sites, site4, site5 } from './mocks';

const Component = ({ items = sites }: { items?: ISiteSnippetType[] }) => (
    <AppProvider>
        <MortgageSites items={items} searchQuery={{}} />
    </AppProvider>
);

describe('MortgageSites', () => {
    it('Рисует карусель с новостройками', async () => {
        await render(<Component />, { viewport: { width: 360, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель с новостройками (горизонтально)', async () => {
        await render(<Component />, { viewport: { width: 700, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель без кнопки если сниппетов меньше 4', async () => {
        await render(<Component items={[site4, site5]} />, { viewport: { width: 360, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
