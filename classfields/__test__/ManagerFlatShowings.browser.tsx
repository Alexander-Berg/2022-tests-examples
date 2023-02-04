import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rootReducer } from 'view/entries/manager/reducer';
import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { ManagerFlatContainer } from '../container';
import { ManagerFlatTab } from '../types';

import {
    getStore,
    allShowingTypes,
    longDescriptionShowing,
    multicontactShowing,
    onlineShowing,
    storeNoShowings,
} from './stubs/showings';

const viewports = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 900 } }];

const FlatForm: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerFlatContainer tab={ManagerFlatTab.SHOWINGS} />
    </AppProvider>
);

describe('ManagerFlatShowings', () => {
    describe('Все статусы показа', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={getStore(allShowingTypes)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Длинное описание', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={getStore(longDescriptionShowing)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Много контактов', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={getStore(multicontactShowing)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Онлайн показ', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={getStore(onlineShowing)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Нет показов для данной квартиры', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={storeNoShowings} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
