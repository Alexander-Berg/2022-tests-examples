import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { SamoletPlansContainer } from '../container';
import styles from '../styles.module.css';

import { baseState, loadingMoreState, loadingState, hasMoreState, emptyListState, planModalState } from './mocks';

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
    <AppProvider initialState={state}>
        <SamoletPlansContainer onSubmit={noop} onFiltersReset={noop} />
    </AppProvider>
);

describe('SamoletPlans', () => {
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

    it('рендерится попап планировок', async () => {
        await render(<Component state={planModalState} />, { viewport: { width: 1300, height: 500 } });

        await page.click(`.${styles.plansSerp} div:nth-child(2) .Button`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
