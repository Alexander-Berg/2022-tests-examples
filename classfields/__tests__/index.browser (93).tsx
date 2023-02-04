import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import set from 'lodash/set';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FlatUserRole } from 'types/flat';

import { AssignStatus } from 'types/assignment';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import 'view/styles/common.css';

import { UserFlatAssignmentContainer } from '../container';

import { getStore } from './stub/store';

const renderOptions = [{ viewport: { width: 960, height: 500 } }, { viewport: { width: 625, height: 500 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = (props) => (
    <AppProvider initialState={props.store}>
        <UserFlatAssignmentContainer />
    </AppProvider>
);

describe('UserFlatAssignment', () => {
    describe(`Корректная ссылка. OWNER`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={getStore()} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Корректная ссылка. TENANT_CANDIDATE`, () => {
        renderOptions.forEach((renderOption) => {
            const store = getStore();

            set(store, 'page.params.role', FlatUserRole.TENANT_CANDIDATE);

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Ссылка устарела.`, () => {
        renderOptions.forEach((renderOption) => {
            const store = getStore();

            set(store, 'assignmentFlat.status', AssignStatus.LINK_EXPIRED);

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Некорректная ссылка.`, () => {
        renderOptions.forEach((renderOption) => {
            const store = getStore();

            set(store, 'assignmentFlat.status', AssignStatus.INVALID_URL);

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Не нашли квартиру ссылка.`, () => {
        renderOptions.forEach((renderOption) => {
            const store = getStore();

            set(store, 'assignmentFlat.status', AssignStatus.UNKNOWN_FLAT);

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Не корректная подпись.`, () => {
        renderOptions.forEach((renderOption) => {
            const store = getStore();

            set(store, 'assignmentFlat.status', AssignStatus.INVALID_SIGN);

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
