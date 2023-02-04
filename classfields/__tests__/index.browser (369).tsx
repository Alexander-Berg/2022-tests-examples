import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferCardBuildingPurposes } from '../index';

import {
    commercialOfferWithPurposes,
    commercialOfferWithWarehousePurposes,
    commercialOfferWithoutPurposes,
} from './mocks';

describe('OfferCardBuildingPurposes', () => {
    it('Отрисовка с назначениями', async () => {
        await render(<OfferCardBuildingPurposes offer={commercialOfferWithPurposes} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с назначениями склада', async () => {
        await render(<OfferCardBuildingPurposes offer={commercialOfferWithWarehousePurposes} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка без назначений', async () => {
        await render(<OfferCardBuildingPurposes offer={commercialOfferWithoutPurposes} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
