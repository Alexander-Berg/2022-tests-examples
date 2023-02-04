import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';

import { IUniversalStore } from 'view/modules/types';
import { IWithMobileProps } from 'view/enhancers/withMobile';

import { InventoryDefectsList, IInventoryDefectsListProps } from '../';

import { defects } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 360, height: 1200 } },
];

const Component: React.FunctionComponent<
    Omit<IInventoryDefectsListProps, keyof IWithMobileProps> & { store: IUniversalStore }
> = ({ store, ...props }) => (
    <div style={{ padding: '20px' }}>
        <AppProvider rootReducer={rootReducer} initialState={store} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
            <InventoryDefectsList {...props} />
        </AppProvider>
    </div>
);

describe('InventoryDefectsList', () => {
    describe(`Базовый вид`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        defects={defects}
                        onAddDefect={() => {
                            return;
                        }}
                        onDefectClick={() => {
                            return;
                        }}
                        store={
                            {
                                config: { isMobile: renderOption.viewport.width === 360 ? 'iOS' : '' },
                            } as IUniversalStore
                        }
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
