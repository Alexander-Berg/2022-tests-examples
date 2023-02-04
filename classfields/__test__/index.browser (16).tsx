import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OutstaffRoles } from 'types/outstaff';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/outstaff/reducer';
import { IUniversalStore } from 'view/modules/types';
import filtersStyles from 'view/components/SearchFlatsByPresetFilters/styles.module.css';

import { OutstaffSearchFlatsContainer } from '../container';

import * as stubs from './stubs/store';

const renderOptions = {
    desktop: {
        viewport: {
            width: 1400,
            height: 1000,
        },
    },
    mobile: {
        viewport: {
            width: 375,
            height: 900,
        },
    },
};

const selectors = {
    queryInput: '#filters-address',
    presetSelect: '#filters-preset',
    presetItem: (n: number) => `.Menu .${filtersStyles.item}:nth-child(${n})`,
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; role?: OutstaffRoles }> = ({ store, role }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store}>
            <OutstaffSearchFlatsContainer role={role || OutstaffRoles.retoucher} />
        </AppProvider>
    );
};

describe('OutstaffSearchFlats', () => {
    describe('Cкелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withSkeletonStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Нет квартир', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Найдено до 10 ти квартир', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(5)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Найдено 10 квартир', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(10)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Найдено 23 квартиры', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(23)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Открыта вторая страница в пагинаторе', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(30, 2)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Открыта последняя страница в пагинаторе', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(30, 3, 5)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Смена фильтров', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStoreWithFlats(1)} />, option);

                await page.type(selectors.queryInput, 'город Москва ул');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.presetSelect);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.presetItem(1));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Список доступных пресетов', () => {
        Object.values(renderOptions).forEach((option) => {
            [OutstaffRoles.photographer, OutstaffRoles.retoucher, OutstaffRoles.copywriter].forEach((role) => {
                it(`width${option.viewport.width}px у ${role}`, async () => {
                    await render(<Component role={role} store={stubs.getStoreWithRole(role)} />, option);

                    await page.click(selectors.presetSelect);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
