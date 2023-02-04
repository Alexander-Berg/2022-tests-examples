import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CardPlansOffersFilters } from '../index';

import styles from '../styles.module.css';

import { getTurnoverOccurrence } from './mocks';

describe('CardPlansOffersFilters', () => {
    it('рисует закрытый селект выбора срока сдачи и корпуса, с двумя выбранными значениями', async() => {
        await render(
            <CardPlansOffersFilters
                selectedHouses={[ '1990069', '1990070' ]}
                turnoverOccurrence={getTurnoverOccurrence()}
            />,
            { viewport: { width: 360, height: 90 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытый селект выбора срока сдачи и корпуса, с двумя выбранными значениями', async() => {
        await render(
            <CardPlansOffersFilters
                selectedHouses={[ '1990069', '1990070' ]}
                turnoverOccurrence={getTurnoverOccurrence()}
            />,
            { viewport: { width: 360, height: 160 } }
        );

        await page.click(`.${styles.houseSelect}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
