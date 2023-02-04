import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { TenantQuestionnaireModerationStatus } from 'types/user';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { TenantQuestionnairePreviewContainer } from '../container';

import { filledQuestionnaireStore, skeletonStore } from './store';

const renderOptions = [{ viewport: { width: 1000, height: 800 } }, { viewport: { width: 415, height: 800 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider initialState={store} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <TenantQuestionnairePreviewContainer />
    </AppProvider>
);

describe('TenantQuestionnairePreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`Жилец с полностью заполненной анкетой ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledQuestionnaireStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Ошибка модерации ${renderOption.viewport.width} px`, async () => {
                const questionnaireModerationErrorStore = {
                    ...filledQuestionnaireStore,
                    legacyUser: {
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
                    },
                };

                await render(<Component store={questionnaireModerationErrorStore} />, renderOption);

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
