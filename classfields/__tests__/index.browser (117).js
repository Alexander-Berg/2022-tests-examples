import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardSlider from '../';

import { developerWithOneSlide, developerWithSeveralSlides } from './mocks';

describe('DeveloperCardSlider', () => {
    it('рисует одиночный слайд с картинкой', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithOneSlide} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует первый слайд с картинкой и контролы', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует слайд с заголовком после клика на правую стрелку', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__arrowRight');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует слайд с кнопкой после клика на левую стрелку', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__arrowLeft');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку другого цвета при наведении на слайде с кнопкой', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__arrowLeft');
        await page.hover('.DeveloperCardSlider__slideButton');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует стрелку другого цвета при наведении на нее', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__arrowLeft');
        await page.hover('.DeveloperCardSlider__arrowLeft');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует слайд с преимуществами после клика на третью точку', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__bullet:nth-child(3)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует точку другого цвета при наведении на нее', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithSeveralSlides} />
            </AppProvider>,
            { viewport: { width: 800, height: 550 } }
        );

        await page.click('.DeveloperCardSlider__arrowLeft');
        await page.hover('.DeveloperCardSlider__bullet:nth-child(3)');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
