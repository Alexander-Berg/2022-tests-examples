import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { GalleryStripe } from '../';
import { IGalleryStripeProps } from '../types';

const SLIDES = [
    {
        src: generateImageUrl({
            width: 250,
            height: 230,
        }),
        src2x: generateImageUrl({
            width: 500,
            height: 460,
        }),
    },
    {
        src: generateImageUrl({
            width: 198,
            height: 280,
        }),
        src2x: generateImageUrl({
            width: 397,
            height: 560,
        }),
    },
    {
        src: generateImageUrl({
            width: 210,
            height: 280,
        }),
        src2x: generateImageUrl({
            width: 420,
            height: 560,
        }),
    },
    {
        src: generateImageUrl({
            width: 330,
            height: 260,
        }),
        src2x: generateImageUrl({
            width: 330,
            height: 260,
        }),
    },
];

const Component = (props: IGalleryStripeProps) => (
    <div style={{ height: '232px', marginBottom: '10px' }}>
        <GalleryStripe {...props} />
    </div>
);

describe('GalleryStripe', () => {
    it('Рисует загруженную галерею', async () => {
        await render(<Component slides={SLIDES} />, { viewport: { width: 320, height: 300 } });

        await page.waitFor(600);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует состояние загрузки', async () => {
        await render(<Component slides={SLIDES} />, { viewport: { width: 320, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует предзагруженный слайд сразу (для оптимизации)', async () => {
        await render(<Component preloadStartImage slides={SLIDES} />, { viewport: { width: 320, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Скроллит карусель до слайда, который нужно показать', async () => {
        await render(<Component startIndex={1} slides={SLIDES} />, { viewport: { width: 320, height: 300 } });

        await page.waitFor(600);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Грузится только во вьюпорте', async () => {
        await render(
            <>
                <Component slides={SLIDES} />
                <Component slides={SLIDES} />
                <Component slides={SLIDES} />
            </>,
            { viewport: { width: 320, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.waitFor(600);

        await page.evaluate(() => {
            window.scrollBy(0, window.innerHeight);
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.waitFor(600);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Может выводить любой другой контент в слайде, помимо изображений', async () => {
        const slides = [
            {
                renderContent() {
                    return <div style={{ background: 'red', width: '200px', height: '100%' }} />;
                },
            },
            ...SLIDES,
        ];

        await render(<Component slides={slides} />, { viewport: { width: 320, height: 300 } });

        await page.waitFor(600);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Карусель скролится при взаимодействии', async () => {
        await render(<Component slides={SLIDES} />, { viewport: { width: 320, height: 300 } });

        await page.waitFor(600);
        await page.evaluate(() => {
            document.querySelector(`div[class^="GalleryStripe__container"]`)!.scroll(200, 0);
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
