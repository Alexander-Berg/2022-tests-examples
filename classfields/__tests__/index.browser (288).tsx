import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { SamoletCatalogContainer } from '../container';

import { baseState, loadingMoreState, hasMoreState, emptyListState, sliderState } from './mocks';

const viewports = [
    { width: 320, height: 400 },
    { width: 380, height: 400 },
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
        <SamoletCatalogContainer navigateToMap={noop} />
    </AppProvider>
);

describe('SamoletSerp', () => {
    it('рендерится корректно', async () => {
        await renderMultiple(<Component state={baseState} />);
    });

    it('рендерится с пустой выдачей', async () => {
        await render(<Component state={emptyListState} />, { viewport: { width: 380, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится с кнопкой подгрузки', async () => {
        await render(<Component state={hasMoreState} />, { viewport: { width: 380, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится в состоянии подгрузки', async () => {
        await render(<Component state={loadingMoreState} />, { viewport: { width: 380, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится со слайдером', async () => {
        await render(<Component state={sliderState} />, { viewport: { width: 380, height: 400 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
