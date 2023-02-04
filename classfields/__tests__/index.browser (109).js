import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import CardPlansOffersFilters from '../index';

const turnoverOccurrence = [ {
    commissioningDate: {
        year: 2021,
        quarter: 2,
        constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
    },
    houseId: '1990069',
    timespan: 'YEAR',
    houseName: 'Корпус 6'
},
{
    commissioningDate: {
        year: 2021,
        quarter: 2,
        constructionState: 'CONSTRUCTION_STATE_FINISHED'
    },
    houseId: '1990070',
    timespan: 'YEAR',
    houseName: 'Корпус 7'
} ];

describe('CardPlansOffersFilters', () => {
    it('рисует открытый селект выбора срока сдачи и корпуса, с двумя выбранными значениями', async() => {
        const selectedHouses = [ '1990069', '1990070' ];

        await render(
            <CardPlansOffersFilters
                selectedHouses={selectedHouses}
                turnoverOccurrence={turnoverOccurrence}
            />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click('.CardPlansOffersFilters__select');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
