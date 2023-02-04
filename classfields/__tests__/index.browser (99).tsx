import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import ModalDisplay from 'view/components/ModalDisplay';
import { userReducer } from 'view/entries/user/reducer';

import { UserPersonalDataFormContainer } from '../container';

import { store, mobileStore, onlyContentStore, skeletonStore } from './stub/store';

const renderOptions = [
    { viewport: { width: 1000, height: 900 } },
    { viewport: { width: 630, height: 900 } },
    { viewport: { width: 375, height: 900 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider initialState={store} rootReducer={userReducer} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <UserPersonalDataFormContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('UserPersonalDataForm', () => {
    describe('Базовое состояние', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(
                    <Component store={renderOption.viewport.width > 800 ? store : mobileStore} />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`OnlyContent ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={onlyContentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Скелетон ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
