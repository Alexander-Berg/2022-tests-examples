import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import omit from 'lodash/omit';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { FlatShowingStatus } from 'types/showing';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/manager/reducer';

import { ManagerFlatShowingTab } from '../types';
import { ManagerFlatShowingContainer } from '../container';
import styles from '../ManagerFlatShowingResolutionForm/styles.module.css';

import * as store from './stub/store';

const renderOptions = [{ viewport: { width: 1280, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    inputs: {
        status: '#STATUS',
        comment: '#COMMENT',
        rejectionReason: '#REJECTION_REASON',
        responsibleManager: '#RESPONSIBLE_MANAGER',
    },
    modalButton: `.${styles.button}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <ManagerFlatShowingContainer tab={ManagerFlatShowingTab.RESOLUTION} />
    </AppProvider>
);

describe('ManagerFlatShowingResolutionForm', () => {
    describe('Рендер всех статусов', () => {
        renderOptions.forEach((renderOption) => {
            Object.keys(
                omit(FlatShowingStatus, [
                    FlatShowingStatus.UNKNOWN,
                    FlatShowingStatus.UNRECOGNIZED,
                    FlatShowingStatus.OWNER_AGREEMENTS,
                    FlatShowingStatus.VALIDATE,
                    FlatShowingStatus.PREPARE_ACCOUNT,
                    FlatShowingStatus.OFFLINE,
                    FlatShowingStatus.ONLINE,
                    FlatShowingStatus.TIME_MATCHING,
                ])
            ).forEach((status) => {
                it(`${status} ${renderOption.viewport.width}px`, async () => {
                    await render(
                        <Component store={store.getSpecificStatusStore(status as FlatShowingStatus)} />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('заполнение формы', () => {
        describe('жилец планирует снимать', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={store.storeStatusShowingAppointed} />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const statusSelect = await page.$(selectors.inputs.status);

                await statusSelect?.focus();
                await statusSelect?.press('Enter');
                await statusSelect?.press('ArrowDown');
                await statusSelect?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.comment, 'Хочет заехать уже сегодня');

                const managerSelect = await page.$(selectors.inputs.responsibleManager);

                await managerSelect?.focus();
                await managerSelect?.press('Enter');
                await managerSelect?.press('ArrowDown');
                await managerSelect?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('жилец не планирует снимать', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={store.storeStatusShowingAppointed} />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const statusSelect = await page.$(selectors.inputs.status);

                await statusSelect?.focus();
                await statusSelect?.press('Enter');
                await statusSelect?.press('ArrowDown');
                await statusSelect?.press('ArrowDown');
                await statusSelect?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.modalButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const rejectionReason = await page.$(selectors.inputs.rejectionReason);

                await rejectionReason?.focus();
                await rejectionReason?.press('Enter');
                await rejectionReason?.press('ArrowDown');
                await rejectionReason?.press('ArrowDown');
                await rejectionReason?.press('Enter');

                const managerSelect = await page.$(selectors.inputs.responsibleManager);

                await managerSelect?.focus();
                await managerSelect?.press('Enter');
                await managerSelect?.press('ArrowDown');
                await managerSelect?.press('ArrowDown');
                await managerSelect?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
