import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import sectionStyles from 'view/components/UserPaymentsHistory/UserPaymentsHistorySection/styles.module.css';
import accordionStyles from 'view/components/AccordionBlock/styles.module.css';

import { UserPaymentsHistoryContainer } from '../container';

import {
    tenantStore,
    outdatedTenantStore,
    ownerStore,
    ownerStoreWithPaging,
    outdatedOwnerStore,
    skeletonStore,
} from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 900 } }, { viewport: { width: 375, height: 900 } }];

const selectors = {
    item: `.${sectionStyles.section} .${accordionStyles.title}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider
        initialState={store}
        fakeTimers={{
            now: new Date('2021-11-12T03:00:00.111Z').getTime(),
        }}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <UserPaymentsHistoryContainer />
    </AppProvider>
);

describe('UserPaymentsHistory', () => {
    describe('Платежи жильца', () => {
        renderOptions.forEach((renderOption) => {
            it(`Список платежей жильца ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={tenantStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Список платежей жильца с раскрытым платежом ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={tenantStore} />, renderOption);

                await page.click(selectors.item);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Список платежей жильца с просрочкой ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={outdatedTenantStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Скелетон ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Платежи собственника', () => {
        renderOptions.forEach((renderOption) => {
            it(`Список платежей собственника ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={ownerStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Список платежей собственника с раскрытым платежом ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={ownerStore} />, renderOption);

                await page.click(selectors.item);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            // eslint-disable-next-line max-len
            it(`Список платежей собственника с просрочкой (без бейджа) ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={outdatedOwnerStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Список платежей с пагинацией ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={ownerStoreWithPaging} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
