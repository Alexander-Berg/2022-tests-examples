import React from 'react';

import { DeepPartial } from 'utility-types';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/public/reducer';
import itemStyles from 'view/components/Navbar/NavbarMenu/NavbarMenuItem/styles.module.css';

import { INavbarMenuPopupProps, NavbarMenuPopup } from '../index';

import * as stubs from './stubs';

const renderOptions = {
    desktop: {
        viewport: {
            width: 1300,
            height: 900,
        },
        isMatchMedia: false,
    },
    iphoneX: {
        viewport: {
            width: 370,
            height: 800,
        },
        isMatchMedia: true,
    },
};

const selectors = {
    itemContainer: `.${itemStyles.container}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; props: INavbarMenuPopupProps }> = ({
    store,
    props,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <NavbarMenuPopup {...props} />
    </AppProvider>
);

describe('NavBarMenuPopup', () => {
    describe('в профиле менеджера', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.managerProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле собственника', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.ownerProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле фотографа', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.photographerProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле жильца', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.tenantProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('новый профиль', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.newProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле колл-центра', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.callCenterProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле копирайтера', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.copywriterProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в профиле ретушёра', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.retoucherProfileProps, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('в наличии еще два аккаунта', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.hasOtherAccounts, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('нет кнопки добавить пользователя', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.notVisibleAddAccountButton, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('скрываем пункт про отзывы если пользователь не соб или жилец', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(
                    <Component
                        store={stubs.baseStore}
                        props={stubs.getPropsWithTouch(stubs.withoutReviewPermission, option.isMatchMedia)}
                    />,
                    option
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('ховер на элемент меню', () => {
        const option = renderOptions.desktop;

        it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
            await render(
                <Component
                    store={stubs.baseStore}
                    props={stubs.getPropsWithTouch(stubs.managerProfileProps, option.isMatchMedia)}
                />,
                option
            );

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.hover(selectors.itemContainer);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });
});
