import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IGalleryV2Props } from '../types';
import { GalleryV2 } from '../index';
import galleryStyles from '../styles.module.css';

import { badges, planImage, tourImage, genPlanImage, baseImages, youtubePreviewImage, wideImages } from './mocks';
import styles from './styles.module.css';

const GalleryInContainer: React.FC<IGalleryV2Props> = (props) => (
    <div style={{ width: '640px', height: '440px' }}>
        <GalleryV2 {...props} className={styles.gallery} />
    </div>
);

describe('GalleryV2', () => {
    it('Базовая отрисовка', async () => {
        await render(<GalleryInContainer images={baseImages} />, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с бейджами', async () => {
        await render(<GalleryInContainer images={baseImages} badges={badges} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка в случае, если изображение только одно', async () => {
        await render(<GalleryInContainer images={baseImages.slice(0, 1)} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка широких изображений', async () => {
        await render(<GalleryInContainer images={wideImages} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с превью видео', async () => {
        await render(<GalleryInContainer images={[youtubePreviewImage].concat(baseImages)} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с туром', async () => {
        await render(<GalleryInContainer images={[tourImage].concat(baseImages)} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с генпланом', async () => {
        await render(<GalleryInContainer images={[genPlanImage, ...baseImages]} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с планировкой', async () => {
        await render(<GalleryInContainer images={[planImage].concat(baseImages)} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Контролы скролла не появляются, если все изображения помещаются в текущий вьюпорт', async () => {
        await render(<GalleryInContainer images={[baseImages[0], baseImages[0]]} />, {
            viewport: { width: 700, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('При нажатии на контрол вперёд происходит скролл к следующим слайдам', async () => {
        await render(<GalleryInContainer images={baseImages} />, { viewport: { width: 700, height: 500 } });

        await page.click(`.${galleryStyles.controlNextImage}`);
        await page.waitFor(600);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('При нажатии на контрол назад происходит скролл к предыдущим слайдам', async () => {
        await render(<GalleryInContainer images={baseImages} />, { viewport: { width: 700, height: 500 } });

        await page.click(`.${galleryStyles.controlNextImage}`);
        await page.waitFor(600);
        await page.click(`.${galleryStyles.controlPrevImage}`);
        await page.waitFor(600);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Поддержка нативного скролла', async () => {
        await render(<GalleryInContainer images={baseImages} />, { viewport: { width: 700, height: 500 } });

        await page.evaluate((containerSelector) => {
            document.querySelector(containerSelector)!.scroll(400, 0);
        }, `.${galleryStyles.slidesContainer}`);
        await page.waitFor(600);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
