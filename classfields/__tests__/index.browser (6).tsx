import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';

import { HouseServiceCounterImageUploader, IHouseServiceCounterImageUploaderProps } from '../';

import * as stubs from './stubs';

const Component: React.FC<{
    store: DeepPartial<IUniversalStore>;
    props?: Omit<IHouseServiceCounterImageUploaderProps, 'isMobile'>;
}> = ({ store, props }) => (
    <AppProvider initialState={store} rootReducer={rootReducer}>
        <HouseServiceCounterImageUploader {...props} />
    </AppProvider>
);

const testOptions = {
    desktop: {
        renderOptions: {
            viewport: {
                width: 888,
                height: 700,
            },
        },
    },
    mobile: {
        renderOptions: {
            viewport: {
                width: 375,
                height: 700,
            },
        },
    },
};

describe('HouseServiceCounterImageUploader', () => {
    describe('Базовый рендеринг есть фото', () => {
        Object.values(testOptions).forEach(({ renderOptions }) => {
            it(`${renderOptions.viewport.width}px`, async () => {
                await render(<Component store={stubs.withUploadedImage} />, renderOptions);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });
});

describe('Базовый рендеринг нет фото', () => {
    Object.values(testOptions).forEach(({ renderOptions }) => {
        it(`${renderOptions.viewport.width}px`, async () => {
            await render(<Component store={stubs.withoutUploadedImage} />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
