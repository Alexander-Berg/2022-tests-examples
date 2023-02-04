import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';

import cardBaseStyles from 'view/components/UserPaymentMethodsCardBase/styles.module.css';
import confirmActionModalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';
import { sberbankCard } from 'view/components/UserPaymentMethodsCardBase/__tests__/stub';

import { OwnerPaymentMethodsContainer } from '../container';

import * as s from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 500 } }, { viewport: { width: 375, height: 300 } }];

const Component: React.FunctionComponent<
    {
        store: DeepPartial<IUniversalStore>;
        Gate?: AnyObject;
    } & React.ComponentProps<typeof OwnerPaymentMethodsContainer>
> = ({ store, Gate, ...otherProps }) => (
    <AppProvider
        rootReducer={userReducer}
        Gate={Gate}
        initialState={store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerPaymentMethodsContainer {...otherProps} />
        <ModalDisplay />
    </AppProvider>
);

const selectors = {
    deleteIcon: (n: number) => `.${cardBaseStyles.container}:nth-child(${n}) .${cardBaseStyles.icon}`,
    confirmDeleteButton: `.${confirmActionModalStyles.buttons} .${confirmActionModalStyles.button}:first-of-type`,
    cancelDeleteButton: `.${confirmActionModalStyles.buttons} .${confirmActionModalStyles.button}:last-of-type`,
};

describe('OwnerPaymentMethodsContainer', () => {
    describe('Внешний вид', () => {
        describe(`Без карт`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={s.storeWithoutCards} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`С одной картой`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={s.storeWithOneCard} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`С двумя картами`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={s.storeWithTwoCard} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Попап удаления открывается и закрывается`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={s.storeWithTwoCard} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.deleteIcon(1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.cancelDeleteButton);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Скелетон`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={s.storeWithSkeleton} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Логика удаления', () => {
        it(`С одной картой`, async () => {
            const Gate = {
                create: () => Promise.resolve({ cards: [] }),
            };

            await render(<Component Gate={Gate} store={s.storeWithOneCard} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.deleteIcon(1));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmDeleteButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`С двумя картами`, async () => {
            const Gate = {
                create: () => Promise.resolve({ cards: [sberbankCard] }),
            };

            await render(<Component Gate={Gate} store={s.storeWithTwoCard} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.deleteIcon(1));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmDeleteButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`В процессе удаления`, async () => {
            const Gate = {
                create: () => new Promise(noop),
            };

            await render(<Component Gate={Gate} store={s.storeWithOneCard} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.deleteIcon(1));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmDeleteButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Не удалось удалить`, async () => {
            const Gate = {
                create: () => Promise.reject(),
            };

            await render(<Component Gate={Gate} store={s.storeWithOneCard} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.deleteIcon(1));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmDeleteButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
