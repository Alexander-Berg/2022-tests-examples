import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISlide } from 'realty-core/types/samolet/slide';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import styles from 'realty-core/view/react/common/components/DotsNavigation/styles.module.css';

import { SamoletSlider } from '../';

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
                <div style={{ margin: -20 }}>
                    <SamoletSlider slides={oneSlide} />
                </div>
            </AppProvider>,
            { viewport: { width: 360, height: 420 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('Много слайдов', () => {
        it('Булеты', async () => {
            await render(
                <AppProvider>
                    <div style={{ margin: -20 }}>
                        <SamoletSlider slides={manySlides} />
                    </div>
                </AppProvider>,
                { viewport: { width: 420, height: 420 } }
            );

            await page.click(`.${styles.dot}:nth-child(2)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(`.${styles.dot}:nth-child(3)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(`.${styles.dot}:nth-child(4)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(`.${styles.dot}:nth-child(5)`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
