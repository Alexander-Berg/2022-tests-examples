import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardSlider from '../';

import {
    developerWithPictureSlide,
    developerWithButtonSlide,
    developerWithFeaturesSlide,
    developerWithTitleSlide
} from './mocks';

describe('DeveloperCardSlider', () => {
    it('рисует слайд с картинкой', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithPictureSlide} />
            </AppProvider>,
            { viewport: { width: 400, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует слайд с кнопкой', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithButtonSlide} />
            </AppProvider>,
            { viewport: { width: 320, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует слайд с преимуществами', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithFeaturesSlide} />
            </AppProvider>,
            { viewport: { width: 640, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует слайд с заголовком', async() => {
        await render(
            <AppProvider initialState={{}}>
                <DeveloperCardSlider developer={developerWithTitleSlide} />
            </AppProvider>,
            { viewport: { width: 450, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
