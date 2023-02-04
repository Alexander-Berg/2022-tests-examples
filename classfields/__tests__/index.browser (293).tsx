import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletPlansFilters } from '../';
import styles from '../styles.module.css';

import { searchQuery, fullSearchQuery, plansSiteFilter } from './mocks';

const commonProps = {
    onSubmit: noop,
    plansSiteFilter,
};

describe('SamoletPlansFilters (touch)', () => {
    it('рендерится корректно (закрытые)', async () => {
        await render(
            <AppProvider>
                <SamoletPlansFilters searchQuery={searchQuery} {...commonProps} />
            </AppProvider>,
            {
                viewport: { width: 420, height: 200 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <SamoletPlansFilters searchQuery={searchQuery} {...commonProps} />
            </AppProvider>,
            {
                viewport: { width: 350, height: 600 },
            }
        );
        await page.click(`.${styles.paramsButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно большой экран', async () => {
        await render(
            <AppProvider>
                <SamoletPlansFilters searchQuery={searchQuery} {...commonProps} />
            </AppProvider>,
            {
                viewport: { width: 420, height: 800 },
            }
        );
        await page.click(`.${styles.paramsButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно в заполненном состоянии', async () => {
        await render(
            <AppProvider>
                <SamoletPlansFilters searchQuery={fullSearchQuery} {...commonProps} />
            </AppProvider>,
            {
                viewport: { width: 350, height: 600 },
            }
        );
        await page.click(`.${styles.paramsButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('переключение фильтров', async () => {
        await render(
            <AppProvider>
                <SamoletPlansFilters searchQuery={searchQuery} {...commonProps} />
            </AppProvider>,
            {
                viewport: { width: 350, height: 600 },
            }
        );
        await page.click(`.${styles.paramsButton}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.Select:nth-child(1)`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        await page.click(`.Popup_visible .Menu .Menu__item:nth-of-type(3)`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.CheckboxGroup:nth-child(2) .Checkbox:first-child`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.CheckboxGroup:nth-child(2) .Checkbox:last-child`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus(`.NumberRange:nth-child(3) .TextInput:first-of-type input`);
        await page.keyboard.type('100000');
        await page.focus(`.NumberRange:nth-child(3) .TextInput:last-of-type input`);
        await page.keyboard.type('20000000');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
