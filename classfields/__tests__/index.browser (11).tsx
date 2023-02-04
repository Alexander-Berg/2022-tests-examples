import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';
import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';

import { HouseServicesMeterReadingsPreviewContainer } from '../container';

import { store, skeletonStore } from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = (props) => (
    <AppProvider rootReducer={rootReducer} initialState={props.store}>
        <UserFlatContentWrapper>
            <HouseServicesMeterReadingsPreviewContainer />
        </UserFlatContentWrapper>
    </AppProvider>
);

describe('HouseServicesMeterReadingsPreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
