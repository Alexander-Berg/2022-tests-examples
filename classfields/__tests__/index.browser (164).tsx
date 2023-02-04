import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';
import { IStore } from 'view/common/reducers';
import 'view/deskpad/common.css';

import { EgrnReportsContainer } from '../container';

import mocks from './mocks';

const Component: React.FunctionComponent<{ store: Partial<IStore> }> = ({ store }) => (
    <div style={{ padding: '10px' }}>
        <AppProvider initialState={store}>
            <EgrnReportsContainer />
        </AppProvider>
    </div>
);

const baseWidthDimension = { viewport: { width: 1440, height: 800 } };
const widthWideDimension = { viewport: { width: 1000, height: 800 } };
const dimensions = [baseWidthDimension, widthWideDimension];

const renderSeveralResolutions = async (Component: React.ReactElement, dimensions: typeof baseWidthDimension[]) => {
    for (const dimension of dimensions) {
        await render(Component, dimension);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('EgrnReports', () => {
    it('С отчетами', async () => {
        await renderSeveralResolutions(<Component store={mocks.withReports} />, dimensions);
    });

    it.skip('Без отчетов', async () => {
        await renderSeveralResolutions(<Component store={mocks.default} />, dimensions);
    });

    it('Не удалось загрузить отчеты', async () => {
        await renderSeveralResolutions(<Component store={mocks.error} />, dimensions);
    });
});
