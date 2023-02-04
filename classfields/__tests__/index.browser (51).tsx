import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import 'view/styles/common.css';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { OwnerFlatTenantCandidateGroupsListingContainer } from '../container';

import { filledStore, emptyStore, skeletonStore, filledStoreWithOnlyContent } from './stub/store';

const renderOptions = [
    { viewport: { width: 960, height: 1000 } },
    { viewport: { width: 625, height: 1000 } },
    { viewport: { width: 360, height: 1000 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        initialState={props.store}
        Gate={props.Gate}
        rootReducer={userReducer}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerFlatTenantCandidateGroupsListingContainer />
    </AppProvider>
);

describe('OwnerFlatTenantCandidateGroupsListing', () => {
    describe(`Заполненный листинг`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Пустой листинг`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={emptyStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Вид при only-content`, () => {
        [renderOptions[1], renderOptions[2]].forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={filledStoreWithOnlyContent} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
