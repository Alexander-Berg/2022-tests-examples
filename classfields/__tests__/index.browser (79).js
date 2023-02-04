import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardPromo from '../';

const developerWithPromo = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    video: {
        id: '1',
        name: 'Видео',
        description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
            'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes.'
    }
};

const developerWithLongPromo = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    video: {
        id: '1',
        name: 'Видео',
        description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
            'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. ' +
            'Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. ' +
            'Donec pede justo, fringill.'
    }
};

describe('DeveloperCardPromo', () => {
    it('рисует превью видео с заголовком и коротким описанием', async() => {
        await render(<DeveloperCardPromo developer={developerWithPromo} />,
            { viewport: { width: 320, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует превью видео с заголовком и сокращенным описанием', async() => {
        await render(<DeveloperCardPromo developer={developerWithLongPromo} />,
            { viewport: { width: 400, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует превью видео с заголовком и полным описанием после раскрытия', async() => {
        await render(<DeveloperCardPromo developer={developerWithLongPromo} />,
            { viewport: { width: 640, height: 450 } }
        );

        await page.click('.Shorter__expander');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
