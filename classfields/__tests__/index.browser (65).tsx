import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
// import ModalDisplay from 'view/components/ModalDisplay';

import styles from 'view/components/InventoryDefect/styles.module.css';

import { OwnerInventoryDefectsContainer } from '../container';

import { filledStore, skeletonStore } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 360, height: 1200 } },
];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
}> = (props) => (
    <div>
        <AppProvider
            initialState={props.store}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <OwnerInventoryDefectsContainer />
            {/* <ModalDisplay /> */}
        </AppProvider>
    </div>
);

describe('OwnerInventoryDefects', () => {
    describe(`Базовый рендеринг`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                await page.evaluate(() => {
                    document.documentElement.style.setProperty('--navigation-sidebar-width', '0');
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    it('Ховер на десктопе', async () => {
        await render(<Component store={filledStore} />, renderOptions[0]);

        await page.evaluate(() => {
            document.documentElement.style.setProperty('--navigation-sidebar-width', '0');
        });

        await page.hover(`.${styles.snippet}`);

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
