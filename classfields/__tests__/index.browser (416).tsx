import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { EGRNAddressPurchase } from '../';

import { store } from './mocks';

describe('EGRNAddressPurchase', () => {
    it('рендерится', async () => {
        await render(
            <AppProvider initialState={store}>
                <EGRNAddressPurchase />
            </AppProvider>,
            {
                viewport: { width: 1500, height: 3000 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
