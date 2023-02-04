import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
import UserFeedbackFormStyles from 'view/components/UserFeedback/UserFeedbackForm/styles.module.css';

import { UserFeedbackFormContainer } from '../container';

import { store, skeletonStore } from './stub/store';

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
        <UserFeedbackFormContainer />
    </AppProvider>
);

const selectors = {
    sendSubmit: `.${UserFeedbackFormStyles.submitButton}`,
    ratingItem: '[data-rating="5"]',
    inputs: {
        review: `.${UserFeedbackFormStyles.textArea} textarea`,
    },
};

describe('UserFeedbackForm', () => {
    describe('Внешний вид', () => {
        describe(`Базовое состояние`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Выбрана оценка`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });
                    await page.click(selectors.ratingItem);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Скелетон`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={skeletonStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Показ ошибок', () => {
        describe(`Не заполнено поле "поделитесь мнением"`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });
                    await page.click(selectors.ratingItem);
                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Ошибка пропадает при вводе в поле', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });
                    await page.click(selectors.ratingItem);
                    await page.click(selectors.sendSubmit);
                    await page.type(selectors.inputs.review, 'Все супер!');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение и отправка формы', () => {
        describe(' Форма в процессе сохранения', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    const Gate = {
                        create: () => new Promise(noop),
                    };
                    await render(<Component store={store} Gate={Gate} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });
                    await page.click(selectors.ratingItem);
                    await page.type(selectors.inputs.review, 'Все супер!');
                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        // TODO: Нужно научиться мокать history.back
        describe.skip('Форма успешно сохранена', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    const Gate = {
                        create: () => {
                            return Promise.resolve({
                                text: 'Все ок',
                                rating: 4,
                            });
                        },
                    };
                    await render(<Component store={store} Gate={Gate} />, renderOption);
                    await page.addStyleTag({ content: 'body{padding: 0}' });
                    await page.click(selectors.ratingItem);
                    await page.type(selectors.inputs.review, 'Все супер!');
                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
