import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MetroStationsSeoLinksBlock } from '../index';

import { getProps } from './mocks';

describe('MetroStationsSeoLinksBlock', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <MetroStationsSeoLinksBlock {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
