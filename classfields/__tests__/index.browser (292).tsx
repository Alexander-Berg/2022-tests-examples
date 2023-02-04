import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { SamoletPlansContainer } from '../container';
import styles from '../styles.module.css';

import { baseState, loadingMoreState, hasMoreState, emptyListState, planModalState } from './mocks';

const viewports = [
    { width: 350, height: 2000 },
    { width: 450, height: 2000 },
    { width: 700, height: 2000 },
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
    <AppProvider initialState={state} context={{ router: { params: { section: 'plans' } } }}>
        <SamoletPlansContainer onSubmit={noop} />
    </AppProvider>
);

describe('SamoletPlans', () => {
    it('рендерится корректно', async () => {
        await renderMultiple(<Component state={baseState} />);
    });

    it('рендерится с пустой выдачей', async () => {
        await render(<Component state={emptyListState} />, { viewport: { width: 450, height: 500 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится с кнопкой подгрузки', async () => {
        await render(<Component state={hasMoreState} />, { viewport: { width: 450, height: 2000 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится в состоянии подгрузки', async () => {
        await render(<Component state={loadingMoreState} />, { viewport: { width: 450, height: 2000 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится попап планировок', async () => {
        await render(<Component state={planModalState} />, { viewport: { width: 450, height: 2000 } });

        await page.click(`.${styles.plansSerp} div:nth-child(2) .Button`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится с открытой сортировкой', async () => {
        await render(<Component state={baseState} />, { viewport: { width: 350, height: 2000 } });

        await page.click(`.${styles.sort}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
