import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramsSort } from 'realty-core/types/mortgage/mortgageProgramsFilters';
import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
// eslint-disable-next-line max-len
import { mortgageApplicationFormReducer } from 'realty-core/view/react/modules/mortgage/mortgage-application-form/redux/reducer';

import { MortgageProgramsSerp } from '../';

import styles from '../styles.module.css';
import { IMortgageProgramsSerpProps } from '../types';

import { fivePrograms, twentyPrograms } from './mocks';

const commonParams = {
    isMoreLoading: false,
    onLoadMore: () => undefined,
    onSortChange: () => undefined,
    propertyCost: 0,
    downPaymentSum: 0,
    periodYears: 0,
    user: {} as never,
    subjectFederationRgid: 1,
};

const Component = (props: IMortgageProgramsSerpProps) => (
    <AppProvider rootReducer={createRootReducer({ mortgageApplicationForm: mortgageApplicationFormReducer })}>
        <MortgageProgramsSerp {...props} />
    </AppProvider>
);

describe('MortgageProgramsSerp', () => {
    it('рисует пустую выдачу', async () => {
        await render(<Component mortgagePrograms={[]} totalPrograms={0} {...commonParams} />, {
            viewport: { width: 900, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу с пятью программами', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} />, {
            viewport: { width: 900, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (ховер на программе)', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} />, {
            viewport: { width: 900, height: 500 },
        });

        await page.hover(`.${styles.serp} > div:nth-child(3)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует выдачу (раскрытый сниппет)', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} />, {
            viewport: { width: 900, height: 700 },
        });

        await page.hover(`.${styles.serp} > div:nth-child(3)`);
        await page.click(`.${styles.serp} > div:nth-child(3) button:nth-child(2)`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (с выбранной сортировкой)', async () => {
        await render(
            <Component
                mortgagePrograms={fivePrograms}
                totalPrograms={5}
                {...commonParams}
                sort={MortgageProgramsSort.MORTGAGE_MIN_RATE}
            />,
            {
                viewport: { width: 900, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (ховер на сортировке)', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} />, {
            viewport: { width: 900, height: 500 },
        });

        await page.hover(`.${styles.rate}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует выдачу с отключенной сортировкой (ховер на сортировке)', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} disableSort />, {
            viewport: { width: 900, height: 500 },
        });

        await page.hover(`.${styles.rate}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует выдачу с суммой кредита', async () => {
        await render(<Component mortgagePrograms={fivePrograms} totalPrograms={5} {...commonParams} withCreditSum />, {
            viewport: { width: 900, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (20 сниппетов)', async () => {
        await render(<Component mortgagePrograms={twentyPrograms} totalPrograms={20} {...commonParams} />, {
            viewport: { width: 900, height: 1700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (еще 5 сниппетов)', async () => {
        await render(<Component mortgagePrograms={twentyPrograms} totalPrograms={25} {...commonParams} />, {
            viewport: { width: 900, height: 1700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу (еще 23 сниппетов)', async () => {
        await render(<Component mortgagePrograms={twentyPrograms} totalPrograms={43} {...commonParams} />, {
            viewport: { width: 900, height: 1700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
