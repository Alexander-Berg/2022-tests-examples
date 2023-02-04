import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { YaDealValuationOffersArchiveContainer } from '../container';

import { getStore } from './stubs/store';

const Component = () => (
    <AppProvider initialState={getStore()}>
        <YaDealValuationOffersArchiveContainer />
    </AppProvider>
);

const desktopViewports = [
    { width: 1000, height: 600 },
    { width: 1200, height: 600 },
] as const;

const render = async (component: React.ReactElement) => {
    for (const viewport of desktopViewports) {
        await _render(component, { viewport });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('YaDealValuationOffersArchive', () => {
    it('рендерится корректно c 3 оферами', async () => {
        await render(<Component />);
    });
});
