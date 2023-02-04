import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import MapInfoPopup from '../index';

import { mockedProps } from './mocks';

describe('MapInfoPopup', () => {
    it('Базовая отрисовка', async () => {
        await render(<MapInfoPopup {...mockedProps} />, {
            viewport: {
                width: 350,
                height: 150,
            },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
