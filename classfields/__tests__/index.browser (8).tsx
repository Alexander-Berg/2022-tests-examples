import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';
import addItemStyles from 'view/components/AddItemSnippet/styles.module.css';

import { HouseServiceListContainer } from '../container';

import styles from '../styles.module.css';

import { store, skeletonStore, storeWithoutHouseServices } from './stub';

const renderOptions = [{ viewport: { width: 945, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const selectors = {
    save_button: `.${styles.button}`,
    add_counter: `.${addItemStyles.snippet}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <HouseServiceListContainer onAddCounterClick={noop} onCounterClick={noop} />
        <ModalDisplay />
    </AppProvider>
);

describe('HouseServiceList', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Нет кнопки сохранить при отсутствии счётчиков', () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={storeWithoutHouseServices} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Открытие модалки для добавления счётчика', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.add_counter);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
