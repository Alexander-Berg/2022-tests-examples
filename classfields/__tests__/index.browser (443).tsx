import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IMortgageBankList } from 'realty-core/types/mortgage/mortgageBank';

import { MortgageSearchBanks } from '../';
import styles from '../styles.module.css';

function getBanks(count: number): IMortgageBankList[] {
    return Array(count)
        .fill(null)
        .map((_, index) => {
            return {
                id: index,
                name: `bank_${index}`,
                logo: generateImageUrl({
                    width: 110,
                    height: 26,
                }),
            };
        });
}

const subjectFederationRgid = 1;

describe('MortgageSearchBanks', () => {
    it('рисует блок (мало банков)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(9)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 900, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (банков на 1 больше)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(19)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 900, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (много банков)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(33)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 900, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (раскрытый список)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(33)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 900, height: 500 },
        });

        await page.hover(`.${styles.showMore}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.click(`.${styles.showMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
