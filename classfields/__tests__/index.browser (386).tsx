import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardNewSearchButton } from '../index';

import { offer } from './mocks';

describe('OfferCardNewSearchButton', function () {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <OfferCardNewSearchButton offer={offer} />
            </AppProvider>,
            {
                viewport: { width: 300, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
