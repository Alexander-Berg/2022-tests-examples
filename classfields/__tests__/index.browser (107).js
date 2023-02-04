import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { StreetsBreadcrumbs } from '..';

const regionInfo = { rgid: 587795 };

describe('StreetsBreadcrumbs', () => {
    it('рисует компонент для списка', async() => {
        await render(
            <AppProvider>
                <StreetsBreadcrumbs regionInfo={regionInfo} />
            </AppProvider>,
            { viewport: { width: 600, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент для улицы', async() => {
        await render(
            <AppProvider>
                <StreetsBreadcrumbs
                    regionInfo={regionInfo}
                    streetInfo={{
                        id: 1,
                        name: '3-й Автозаводский проезд'
                    }}
                />
            </AppProvider>,
            { viewport: { width: 800, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
