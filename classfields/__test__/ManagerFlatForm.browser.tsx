import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';

import { rootReducer } from 'view/entries/manager/reducer';

import { IUniversalStore } from 'view/modules/types';

import suggestListStyles from 'view/components/SuggestList/styles.module.css';

import { ManagerFlatContainer } from '../container';
import { ManagerFlatTab } from '../types';

import styles from '../ManagerFlatForm/styles.module.css';

import * as stubs from './stubs/flat';

const selectors = {
    flatAdress: '#FLAT_ADDRESS',
    submitButton: `.${styles.submit}`,
    suggest: {
        itemN: (n: number) => `.${suggestListStyles.list} .${suggestListStyles.item}:nth-child(${n})`,
    },
};

const viewports = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 900 } }];
const FlatForm: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatContainer tab={ManagerFlatTab.FLAT} />
    </AppProvider>
);

describe('ManagerFlat(форма созданной квартиры)', () => {
    describe('рендер скелетона', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={stubs.onSkeleton} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('квартира создана собственником', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={stubs.flatCreatedByOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('есть данные из заявки', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={stubs.flatCreatedByAplication} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Ручка бэкенда ответила успешно', () => {
        viewports.forEach((option) => {
            const Gate = {
                create: () => {
                    return Promise.resolve({
                        flat: {
                            ...stubs.flatCreatedByAplication.managerFlat,
                        },
                    });
                },
            };
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={stubs.flatCreatedByAplication} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submitButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Ручка бэкенда ответила с ошибкой', () => {
        viewports.forEach((option) => {
            const Gate = {
                create: () => {
                    return Promise.reject();
                },
            };
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={stubs.flatCreatedByOwner} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submitButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
