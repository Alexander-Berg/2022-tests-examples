import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardLocation } from '../index';

import { offer } from './mocks';

describe('CardLocation', function () {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <CardLocation item={offer} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
