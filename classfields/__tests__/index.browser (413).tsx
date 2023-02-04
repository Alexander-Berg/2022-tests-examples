import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';

import { DevelopersListTable } from '../index';

import { getDevelopersList, geo } from './mocks';

const tableRef = React.createRef();

describe('DevelopersListTable', () => {
    it('Рендерится корректно - 1000px', async () => {
        await render(
            <AppProvider>
                <DevelopersListTable
                    developers={getDevelopersList()}
                    geo={geo as IGeoStore}
                    tableRef={tableRef as React.RefObject<HTMLDivElement>}
                />
            </AppProvider>,
            {
                viewport: { width: 1000, height: 700 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится корректно - 1400px', async () => {
        await render(
            <AppProvider>
                <DevelopersListTable
                    developers={getDevelopersList()}
                    geo={geo as IGeoStore}
                    tableRef={tableRef as React.RefObject<HTMLDivElement>}
                />
            </AppProvider>,
            {
                viewport: { width: 1400, height: 700 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерит заглушку пустого листинга', async () => {
        await render(
            <AppProvider>
                <DevelopersListTable
                    developers={[]}
                    geo={geo as IGeoStore}
                    tableRef={tableRef as React.RefObject<HTMLDivElement>}
                />
            </AppProvider>,
            {
                viewport: { width: 1000, height: 700 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
