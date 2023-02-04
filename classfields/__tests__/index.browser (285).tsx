import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { RailwaysBreadcrumbs } from '../index';

const regionInfo = { rgid: 587795 };

describe('RailwaysBreadcrumbs', () => {
    it('рисует компонент для списка станций', async () => {
        await render(
            <AppProvider>
                <RailwaysBreadcrumbs type="railways" regionInfo={regionInfo} />
            </AppProvider>,
            { viewport: { width: 320, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент для станции', async () => {
        await render(
            <AppProvider>
                <RailwaysBreadcrumbs
                    type="railway"
                    regionInfo={regionInfo}
                    railwayInfo={{
                        id: 193324,
                        name: 'Арбат',
                    }}
                />
            </AppProvider>,
            { viewport: { width: 320, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
