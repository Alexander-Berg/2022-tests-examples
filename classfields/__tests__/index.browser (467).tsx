import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { SamoletSerpContainer } from '../container';

import { baseState, loadingMoreState, loadingState, hasMoreState, emptyListState } from './mocks';

const viewports = [
    { width: 1000, height: 400 },
    { width: 1300, height: 400 },
] as const;

const renderMultiple = async (component: React.ReactElement) => {
    for (const viewport of viewports) {
        await render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const Component = ({ state }: { state?: AnyObject }) => (
    <AppProvider
        initialState={state}
        context={{ observeIntersection: () => undefined, unObserveIntersection: () => undefined }}
    >
        <SamoletSerpContainer onLoadMore={noop} onFiltersReset={noop} />
    </AppProvider>
);

describe('SamoletSerp', () => {
    it('рендерится корректно', async () => {
        await renderMultiple(<Component state={baseState} />);
    });

    it('рендерится с пустой выдачей', async () => {
        await render(<Component state={emptyListState} />, { viewport: { width: 1300, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится с кнопкой подгрузки', async () => {
        await render(<Component state={hasMoreState} />, { viewport: { width: 1300, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится в состоянии подгрузки', async () => {
        await render(<Component state={loadingMoreState} />, { viewport: { width: 1300, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится в состоянии загрузки', async () => {
        await render(<Component state={loadingState} />, { viewport: { width: 1300, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
