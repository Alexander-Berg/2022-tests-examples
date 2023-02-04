import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IMortgageBankList } from 'realty-core/types/mortgage/mortgageBank';

import { MortgageSearchBanks } from '../';
import styles from '../styles.module.css';

const subjectFederationRgid = 1;

function getBanks(count: number): IMortgageBankList[] {
    return Array(count)
        .fill(null)
        .map((_, index) => {
            return {
                id: index,
                name: `bank_${index}`,
                logo: generateImageUrl({
                    width: 130,
                    height: 30,
                }),
            };
        });
}

describe('MortgageSearchBanks', () => {
    it('рисует блок (мало банков)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(3)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 320, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (банков на 1 больше)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(7)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 400, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (много банков)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(16)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 640, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (раскрытый список)', async () => {
        await render(<MortgageSearchBanks banks={getBanks(16)} subjectFederationRgid={subjectFederationRgid} />, {
            viewport: { width: 640, height: 400 },
        });

        await page.click(`.${styles.showMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
