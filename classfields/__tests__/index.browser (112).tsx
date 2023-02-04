import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import ItemAddress from '../index';

import * as props from './mocks';

describe('ItemAddress', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <ItemAddress {...props.baseProps} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка без адреса, но с unifiedLocation', async () => {
        await render(
            <AppProvider>
                <ItemAddress {...props.basePropsWithUnifiedLocation} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрабатывает внешний плейслхолдер', async () => {
        await render(
            <AppProvider>
                <ItemAddress {...props.basePropsWithExternalPlaceholder} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Отрабатывает дефолтный плейслхолдер`, async () => {
        await render(
            <AppProvider>
                <ItemAddress {...props.basePropsWithoutDataAndPlaceholder} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
