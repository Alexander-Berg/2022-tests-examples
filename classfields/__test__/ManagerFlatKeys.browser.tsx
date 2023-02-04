import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rootReducer } from 'view/entries/manager/reducer';
import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { ManagerFlatContainer } from '../container';
import { ManagerFlatTab } from '../types';

import { flatCreatedByOwner } from './stubs/flat';

const viewports = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 900 } }];

const FlatForm: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerFlatContainer tab={ManagerFlatTab.KEYS} />
    </AppProvider>
);

describe('ManagerFlatKeys', () => {
    describe('Внешний вид', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatForm store={flatCreatedByOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
