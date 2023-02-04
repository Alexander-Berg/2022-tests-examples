import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SiteStatisticsFilters } from '../';

import styles from '../styles.module.css';

import { oneClientProps, oneSiteProps, noSiteProps, defaultProps, noAgencyProps } from './mocks';

const renderOptions = { viewport: { width: 1300, height: 100 } };

advanceTo(new Date('2021-12-01'));

describe('SiteStatisticsFilters', () => {
    it('Фильтры с одним клиентом', async () => {
        await render(<SiteStatisticsFilters {...oneClientProps} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Фильтры без агентства', async () => {
        await render(<SiteStatisticsFilters {...noAgencyProps} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Фильтры с одним ЖК', async () => {
        await render(<SiteStatisticsFilters {...oneSiteProps} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Фильтры без ЖК', async () => {
        await render(<SiteStatisticsFilters {...noSiteProps} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Переключение фильтров', async () => {
        await render(<SiteStatisticsFilters {...defaultProps} />, { viewport: { width: 1300, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.container} > .${styles.select}:first-child`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.Popup_visible .Menu .Menu__item:nth-of-type(3)`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.container} > .${styles.select}:nth-child(2)`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.Popup_visible .Menu .Menu__item:nth-of-type(3)`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.periodPickerButton}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.Popup_visible [aria-label="day-13"]`);
        await page.click(`.Popup_visible [aria-label="day-19"]`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.Popup_visible .Button`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
