import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import OffersSearchResultsPresets from '../';

import { initialState, suggestions } from './mocks';

describe('OffersSearchResultsPresets', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(
            <AppProvider initialState={initialState}>
                {/* eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
                {/* @ts-ignore */}
                <OffersSearchResultsPresets suggestions={suggestions} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 85 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
