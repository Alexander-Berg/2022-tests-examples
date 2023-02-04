import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Button from 'vertis-react/components/Button';

import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';
import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';

import { HouseServiceNotificationBase, IHouseServiceNotificationBaseProps } from '..';
import styles from '../styles.module.css';

import * as stubs from './stubs';

const selectors = {
    spoilerButton: `.${styles.spoilerButton}`,
};

const renderOptions = {
    desktop: {
        viewport: {
            width: 1200,
            height: 1000,
        },
    },
    mobile: {
        viewport: {
            width: 375,
            height: 800,
        },
    },
};

const renderComponent = (props: IHouseServiceNotificationBaseProps, children?: React.ReactNode) => {
    return (
        <AppProvider rootReducer={userReducer} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
            <UserFlatContentWrapper>
                <HouseServiceNotificationBase {...props}>{children}</HouseServiceNotificationBase>
            </UserFlatContentWrapper>
        </AppProvider>
    );
};

describe('HouseServiceNotificationBase', () => {
    describe('Базовый рендеринг', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(renderComponent(stubs.baseProps), option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть комментарий', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(renderComponent(stubs.propsWithSpoiler), option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Открытие текста комментария', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(renderComponent(stubs.propsWithSpoiler), option);

                await expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.spoilerButton);

                await expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('рендер с кнопкой', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(
                    renderComponent(
                        stubs.propsWithSpoiler,
                        <Button theme="realty" view="yellow" style={{ marginTop: '24px' }}>
                            Сохранить
                        </Button>
                    ),
                    option
                );

                await expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.spoilerButton);

                await expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
