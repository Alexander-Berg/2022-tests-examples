import React from 'react';

import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';
import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/public/reducer';
import { IUniversalStore } from 'view/modules/types';
import { LandingContextProvider } from 'view/enhancers/withLandingContext';

import { LandingTenant } from '../index';

import * as stubs from './stubs';

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <WithScrollContextProvider>
            <LandingContextProvider>
                <LandingTenant />
            </LandingContextProvider>
        </WithScrollContextProvider>
    </AppProvider>
);

const renderOptions = [
    {
        store: stubs.baseStore,
        display: {
            viewport: {
                width: 1400,
                height: 812,
            },
        },
    },
    {
        store: stubs.mobileStore,
        display: {
            viewport: {
                width: 768,
                height: 812,
            },
        },
    },
    {
        store: stubs.mobileStore,
        display: {
            viewport: {
                width: 375,
                height: 812,
            },
        },
    },
];

describe('LandingTenant', () => {
    describe('Базовый рендеринг', () => {
        renderOptions.forEach(({ store, display }) => {
            it(`${display.viewport.width}px ${display.viewport.height}`, async () => {
                await render(<Component store={store} />, display);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
