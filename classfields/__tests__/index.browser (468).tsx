import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ISlide } from 'realty-core/types/samolet/slide';

import dotsStyles from 'realty-core/view/react/common/components/DotsNavigation/styles.module.css';

import { SamoletSlider } from '../';
import styles from '../styles.module.css';

const viewport = { width: 1440, height: 650 };

const oneSlide: ISlide[] = [
    {
        id: '1',
        rgid: 1,
        siteId: '1',
        type: 'text',
        image: generateImageUrl({ width: 2000, height: 900 }),
        title: 'Саларьево парк',
        description: 'Ипотека 4.6% на срок до 30 лет',
        hasButton: true,
        buttonText: 'Выбрать квартиру',
    },
];

const manySlides: ISlide[] = [
    ...oneSlide,
    {
        id: '2',
        rgid: 2,
        siteId: '2',
        type: 'text',
        image: generateImageUrl({ width: 2000, height: 900 }),
        title: 'Белая Дача парк',
        description: '',
        hasButton: true,
        buttonText: 'Выбрать квартиру',
    },
    {
        id: '3',
        rgid: 3,
        siteId: '3',
        type: 'text',
        image: generateImageUrl({ width: 2000, height: 900 }),
        title: '',
        description: 'Ипотека 4.6% на срок до 30 лет',
        hasButton: true,
        buttonText: 'Выбрать квартиру',
    },
    {
        id: '4',
        rgid: 4,
        siteId: '4',
        type: 'text',
        title: 'Саларьево парк',
        description: 'Ипотека 4.6% на срок до 30 лет',
        hasButton: true,
        buttonText: 'Выбрать квартиру',
    },
    {
        id: '5',
        rgid: 5,
        siteId: '5',
        type: 'text',
        image: generateImageUrl({ width: 2000, height: 900 }),
        title: 'Академика Павлова',
        description: 'Ипотека 4.6% на срок до 30 лет',
        hasButton: false,
        buttonText: '',
    },
];

describe('SamoletSlider', () => {
    it('рендерится c единичным слайдом', async () => {
        await render(
            <AppProvider>
                <SamoletSlider slides={oneSlide} />
            </AppProvider>,
            { viewport }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('Много слайдов', () => {
        it('Стрелки', async () => {
            allure.descriptionHtml(`Переключение слайдера -> потом ← ←`);

            await render(
                <AppProvider>
                    <SamoletSlider slides={manySlides} />
                </AppProvider>,
                { viewport }
            );

            await page.hover(`.${styles.arrowRight}`);
            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.click(`.${styles.arrowRight}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.hover(`.${styles.arrowLeft}`);
            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.waitFor(800);
            await page.click(`.${styles.arrowLeft}`);
            await page.waitFor(800);
            await page.click(`.${styles.arrowLeft}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Булеты', async () => {
            allure.descriptionHtml(`Переключение на второй, потом на четвертый`);

            await render(
                <AppProvider>
                    <SamoletSlider slides={manySlides} />
                </AppProvider>,
                { viewport: { width: 1000, height: 600 } }
            );

            await page.click(`.${dotsStyles.dot}:nth-child(2)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(`.${dotsStyles.dot}:nth-child(4)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
