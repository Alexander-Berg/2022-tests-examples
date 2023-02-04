import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';
import { IDevelopersListFilters } from 'realty-core/view/react/modules/developers-list/redux/types';

import { DevelopersListTable } from '../index';

import { getDevelopersListMock, IGetDevelopersListOpts, geo } from './mocks';

const viewports = [
    { width: 320, height: 1000 },
    { width: 360, height: 1000 },
    { width: 375, height: 1000 },
] as const;

const filtersMock: IDevelopersListFilters = {
    selectedBuildingClass: '',
    developer: {},
    yearsRange: [null, null],
};

const optsForPager: IGetDevelopersListOpts = {
    currentPage: 4,
    pageCount: 6,
};

const getProps = (optsForPager: IGetDevelopersListOpts = {}, isListEmpty = false) => ({
    developersList: getDevelopersListMock({ ...optsForPager, noDevelopers: isListEmpty }),
    filters: filtersMock,
    geo: geo as IGeoStore,
    pagesFrom: optsForPager.currentPage ?? 1,
    pagesTo: optsForPager.currentPage ?? 1,
    onClear: () => {
        return;
    },
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onPageChange: (isIncreasing: boolean) => {
        return;
    },
});

describe('DevelopersListTable', () => {
    it('Рендерится корректно', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider>
                    <div style={{ background: 'rgb(243, 243, 246)' }}>
                        <DevelopersListTable {...getProps()} />
                    </div>
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('Рендерит заглушку пустого листинга', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider>
                    <div style={{ background: 'rgb(243, 243, 246)' }}>
                        <DevelopersListTable {...getProps({}, true)} />
                    </div>
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('Рендерит кнопки пагинации', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider>
                    <div style={{ background: 'rgb(243, 243, 246)' }}>
                        <DevelopersListTable {...getProps(optsForPager)} />
                    </div>
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });
});
