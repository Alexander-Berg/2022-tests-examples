import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DistrictBreadcrumbs } from '../index';

const regionInfo = { rgid: 587795 };

describe('DistrictBreadcrumbs', () => {
    it('рисует компонент для списка районов', async () => {
        await render(
            <AppProvider>
                <DistrictBreadcrumbs type="districts" regionInfo={regionInfo} />
            </AppProvider>,
            { viewport: { width: 320, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент для района', async () => {
        await render(
            <AppProvider>
                <DistrictBreadcrumbs
                    type="district"
                    regionInfo={regionInfo}
                    districtInfo={{
                        id: 193324,
                        name: 'Арбат',
                    }}
                />
            </AppProvider>,
            { viewport: { width: 320, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
