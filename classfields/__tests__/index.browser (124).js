import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportPreviousRightsBlock } from '../';
import styles from '../styles.module.css';

const prevRight = {
    owners: [ {
        type: 'NATURAL_PERSON'
    }, {
        type: 'NATURAL_PERSON'
    }, {
        type: 'NATURAL_PERSON'
    } ],
    registration: {
        idRecord: '000000000000',
        regNumber: '00-00-00/000/2020-001',
        type: 'JOINT_OWNERSHIP',
        regDate: '2020-09-07T00:00:00Z',
        endDate: '2020-09-07T00:00:00Z'
    }
};

describe('EGRNPaidReportPreviousRightsBlock', () => {
    it('Рендер блока "Предыдущие собственники": 0 записей', async() => {
        await render(
            <EGRNPaidReportPreviousRightsBlock previousRights={[]} />,
            { viewport: { width: 940, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендер блока "Предыдущие собственники": 1 запись', async() => {
        await render(
            <EGRNPaidReportPreviousRightsBlock previousRights={[ prevRight ]} />,
            { viewport: { width: 940, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендер блока "Предыдущие собственники": несколько записей', async() => {
        await render(
            <EGRNPaidReportPreviousRightsBlock
                previousRights={[ prevRight, prevRight, prevRight, prevRight, prevRight ]}
            />,
            { viewport: { width: 940, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендер блока "Предыдущие собственники": несколько записей (раскрытый спойлер)', async() => {
        await render(
            <EGRNPaidReportPreviousRightsBlock
                previousRights={[ prevRight, prevRight, prevRight, prevRight, prevRight ]}
            />,
            { viewport: { width: 940, height: 500 } }
        );

        await page.click(`.${styles.spoiler}>button`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
