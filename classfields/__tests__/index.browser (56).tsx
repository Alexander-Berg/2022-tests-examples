import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { OwnerHouseServicePreviewContainer } from '../container';

import { store, mobileStore, skeletonStore, onlyContentStore } from './stub/store';

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
            Gate={props.Gate}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <OwnerHouseServicePreviewContainer />
        </AppProvider>
    </div>
);

describe('OwnerHouseServicePreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component store={renderOption.viewport.width === 360 ? { ...store, ...mobileStore } : store} />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`OnlyContent`, () => {
        it(`${renderOptions[2].viewport.width}px`, async () => {
            await render(<Component store={onlyContentStore} />, renderOptions[2]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={
                            renderOption.viewport.width === 360 ? { ...skeletonStore, ...mobileStore } : skeletonStore
                        }
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
