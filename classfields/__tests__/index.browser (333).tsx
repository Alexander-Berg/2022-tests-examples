import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SiteStatisticTypes } from 'types/siteStatistics';

import { SiteStatisticsDashboardChart } from '../';
import { ISiteStatisticsDashboardChartProps } from '../types';

import {
    LESS_THAN_1E6_VALUES,
    GREATER_THAN_1E6_VALUES,
    ZERO_VALUES,
    PERCENT_VALUES,
    MORE_THAN_ONE_MONTH,
    VERY_SMALL_VALUES,
    DEFAULT_DATA_BY_TYPE,
    MORE_THAN_ONE_YEAR,
    SITE_PLACE_WITHOUT_SOME_VALUES,
} from './mocks';
import styles from './styles.module.css';

const Component = ({ data, statisticsType, isSub }: ISiteStatisticsDashboardChartProps) => (
    <div className={styles.container}>
        <SiteStatisticsDashboardChart
            data={data}
            statisticsType={statisticsType}
            className={styles.chart}
            isSub={isSub}
            noAnimation
        />
    </div>
);

describe('SiteStatisticsDashboardChart', () => {
    it('Рендерится с объявлениями до миллиона', async () => {
        await render(<Component data={LESS_THAN_1E6_VALUES} statisticsType={SiteStatisticTypes.OffersNumberByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с объявлениями больше миллиона', async () => {
        await render(
            <Component data={GREATER_THAN_1E6_VALUES} statisticsType={SiteStatisticTypes.OffersNumberByDay} />,
            {
                viewport: { width: 900, height: 400 },
            }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с объявлениями и значениями 0', async () => {
        await render(<Component data={ZERO_VALUES} statisticsType={SiteStatisticTypes.OffersNumberByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с ценой до миллиона', async () => {
        await render(<Component data={LESS_THAN_1E6_VALUES} statisticsType={SiteStatisticTypes.PricePerMeterByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с ценой больше миллиона', async () => {
        await render(
            <Component data={GREATER_THAN_1E6_VALUES} statisticsType={SiteStatisticTypes.PricePerMeterByDay} />,
            {
                viewport: { width: 900, height: 400 },
            }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с ценой и значениями 0', async () => {
        await render(<Component data={ZERO_VALUES} statisticsType={SiteStatisticTypes.PricePerMeterByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с долей', async () => {
        await render(<Component data={PERCENT_VALUES} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с долей значениями 0', async () => {
        await render(<Component data={ZERO_VALUES} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится с периодом > 1 месяца ', async () => {
        await render(
            <Component data={MORE_THAN_ONE_MONTH} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} />,
            {
                viewport: { width: 900, height: 400 },
            }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится c отсутствием данных', async () => {
        await render(<Component data={[]} statisticsType={SiteStatisticTypes.OffersNumberByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится вспомогательный график', async () => {
        await render(
            <Component data={PERCENT_VALUES} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} isSub />,
            {
                viewport: { width: 900, height: 400 },
            }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится график с очень маленькими значениями', async () => {
        await render(<Component data={VERY_SMALL_VALUES} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it.each(Object.values(SiteStatisticTypes))('Метрика %s без данных', async (statistic) => {
        await render(<Component data={ZERO_VALUES} statisticsType={statistic} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it.each(Object.values(SiteStatisticTypes))('Метрика %s с данными', async (statistic) => {
        await render(<Component data={DEFAULT_DATA_BY_TYPE[statistic]} statisticsType={statistic} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рендерится график c годами', async () => {
        await render(
            <Component data={MORE_THAN_ONE_YEAR} statisticsType={SiteStatisticTypes.ClicksPerSubjectByDay} />,
            {
                viewport: { width: 900, height: 400 },
            }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Корректно обрабатывает значение 0 на метрике "Место ЖК"', async () => {
        await render(
            <Component data={SITE_PLACE_WITHOUT_SOME_VALUES} statisticsType={SiteStatisticTypes.PlaceInSubjectByDay} />,
            { viewport: { width: 900, height: 400 } }
        );

        await page.hover('.recharts-area');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
