import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { PersonalDataPreviewContainer } from '../container';

import { filledStore, filledNameAndPhoneStore, skeletonStore } from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 600 } }, { viewport: { width: 375, height: 600 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider initialState={store}>
        <PersonalDataPreviewContainer />
    </AppProvider>
);

describe('PersonalDataPreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`Юзер с полностью заполненными личными данными ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Юзер с заполненным ФИО и телефоном ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledNameAndPhoneStore} />, renderOption);

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
