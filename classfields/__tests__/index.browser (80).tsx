import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';
import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';

import { TenantHouseServicesMeterReadingsPreviewContainer } from '../container';

import { store, canBeChangedStore } from './stub';

const renderOptions = [{ viewport: { width: 1200, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = (props) => (
    <AppProvider
        rootReducer={rootReducer}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <UserFlatContentWrapper>
            <TenantHouseServicesMeterReadingsPreviewContainer />
        </UserFlatContentWrapper>
    </AppProvider>
);

describe('TenantHouseServicesMeterReadingsPreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`Двухтарифный счётчик ${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Показания переданы менее 15 минут назад ${renderOption.viewport.width}px`, async () => {
                await render(<Component store={canBeChangedStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
