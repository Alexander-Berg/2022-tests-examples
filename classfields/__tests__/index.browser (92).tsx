import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { UserFeedbackPreviewContainer } from '../container';

import { store, skeletonStore, withoutCommentStore, longCommentStore, badRatingStore } from './stub/store';

const renderOptions = [
    { viewport: { width: 1280, height: 1000 } },
    { viewport: { width: 375, height: 1000 } },
    { viewport: { width: 320, height: 1000 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        rootReducer={userReducer}
        Gate={props.Gate}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <UserFeedbackPreviewContainer />
    </AppProvider>
);

describe('UserFeedbackPreview', () => {
    describe('Внешний вид', () => {
        describe('Базовое состояние', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Нет ответа', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={withoutCommentStore} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Длинный ответ', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={longCommentStore} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Плохой рейтинг', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={badRatingStore} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Скелетон', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={skeletonStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
