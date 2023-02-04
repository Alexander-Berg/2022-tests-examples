import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';
import { rootReducer } from 'view/entries/user/reducer';

import { OwnerHouseServiceListContainer } from '../container';

import { store, skeletonStore, onlyContentStore, mobileStore } from './stub';

const renderOptions = [{ viewport: { width: 945, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        rootReducer={rootReducer}
        Gate={props.Gate}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <UserFlatContentWrapper isMobileExtended>
            <OwnerHouseServiceListContainer />
        </UserFlatContentWrapper>
    </AppProvider>
);

describe('OwnerHouseServiceList', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component store={renderOption.viewport.width === 375 ? { ...store, ...mobileStore } : store} />,
                    renderOption
                );
                await page.addStyleTag({ content: 'body{padding: 0; padding-bottom: 20px}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`OnlyContent`, () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={onlyContentStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={
                            renderOption.viewport.width === 375 ? { ...skeletonStore, ...mobileStore } : skeletonStore
                        }
                    />,
                    renderOption
                );
                await page.addStyleTag({ content: 'body{padding: 0; padding-bottom: 20px}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
