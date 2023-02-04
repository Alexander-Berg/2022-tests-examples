import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DistrictsBreadcrumbs } from '../index';

const regionInfo = { rgid: 587795 };

describe('DistrictsBreadcrumbs', () => {
    it('рисует компонент для списка районов', async () => {
        await render(
            <AppProvider>
                <DistrictsBreadcrumbs type="districts" regionInfo={regionInfo} />
            </AppProvider>,
            { viewport: { width: 400, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент для района', async () => {
        await render(
            <AppProvider>
                <DistrictsBreadcrumbs
                    type="district"
                    regionInfo={regionInfo}
                    districtInfo={{
                        id: 1,
                        name: 'Центральный',
                    }}
                />
            </AppProvider>,
            { viewport: { width: 700, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
