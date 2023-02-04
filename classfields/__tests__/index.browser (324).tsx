import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { YaDealValuationPriceLineChartReportContainer } from '../container';

import { getStore } from './stubs/store';

const mobileViewports = [
    { width: 345, height: 700 },
    { width: 375, height: 700 },
] as const;

const Component = () => (
    <AppProvider initialState={getStore()}>
        <YaDealValuationPriceLineChartReportContainer />
    </AppProvider>
);

const render = async (component: React.ReactElement) => {
    for (const viewport of mobileViewports) {
        await _render(component, { viewport });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('YaDealValuationPriceLineChartReport(touch)', () => {
    it('рендерится корректно', async () => {
        await render(<Component />);
    });
});
