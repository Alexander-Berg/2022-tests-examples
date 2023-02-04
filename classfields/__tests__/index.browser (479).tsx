import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import ObserverProvider from 'realty-core/view/react/modules/lazy/common/ObserverProvider';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OffersSerpItem from '..';

import { getOfferWith3dTour, statsSelector } from './mocks';

describe('OffersSerpItem', () => {
    it('с 3д туром', async () => {
        await render(
            <AppProvider>
                <ObserverProvider>
                    <OffersSerpItem
                        item={getOfferWith3dTour()}
                        createOfferStatsSelector={statsSelector}
                        createOfferPhoneStatsSelector={statsSelector}
                    />
                </ObserverProvider>
            </AppProvider>,
            { viewport: { width: 900, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
