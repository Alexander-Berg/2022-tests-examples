import React from 'react';

import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';

import { ManagerUserFlatsContainer } from '../container';

import { storeFlats, storeNoFlats, storeSkeleton } from './stub';

const renderOptions = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 900 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerUserFlatsContainer />
    </AppProvider>
);

describe('ManagerUserFlats', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((option) => {
            it(`Пользователь не привязан к квартирам ${option.viewport.width}px`, async () => {
                await render(<Component store={storeNoFlats} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((option) => {
            it(`Приязаны три квартиры ${option.viewport.width}px`, async () => {
                await render(<Component store={storeFlats} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((option) => {
            it(`Скелетон ${option.viewport.width}px`, async () => {
                await render(<Component store={storeSkeleton} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
