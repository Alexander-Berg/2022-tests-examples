import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';

import { OwnerHouseServiceFormContainer } from '../container';

import * as stub from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = (props) => (
    <AppProvider
        rootReducer={rootReducer}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerHouseServiceFormContainer />
    </AppProvider>
);

describe('OwnerHouseServiceForm', () => {
    describe('Внешний вид', () => {
        describe('Форма создания', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.store} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`OnlyContent`, () => {
            it(`${renderOptions[1].viewport.width}px`, async () => {
                await render(<Component store={stub.onlyContentStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('Форма редактирования', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.filledStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
