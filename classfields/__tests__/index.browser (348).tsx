import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MetroBreadcrumbs } from '../index';

const regionInfo = { rgid: 587795 };

describe('MetroBreadcrumbs', () => {
    it('рисует компонент для списка метро', async () => {
        await render(
            <AppProvider>
                <MetroBreadcrumbs type="metro-stations" regionInfo={regionInfo} />
            </AppProvider>,
            { viewport: { width: 400, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент для станции', async () => {
        await render(
            <AppProvider>
                <MetroBreadcrumbs
                    type="metro-station"
                    regionInfo={regionInfo}
                    metroStationInfo={{
                        id: 17,
                        name: 'Московская',
                    }}
                />
            </AppProvider>,
            { viewport: { width: 700, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
