import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import styles from 'realty-core/view/react/common/components/Accordion/AccordionEntry/styles.module.css';

import { EGRNAddressPurchaseFAQ } from '../';

const expanderSelector = `.${styles.arrow}:not(.${styles.arrowRotated}):first-of-type`;

describe('EGRNAddressPurchaseFAQ', () => {
    it('рендерится с закрытими разделами', async () => {
        await render(<EGRNAddressPurchaseFAQ />, {
            viewport: { width: 360, height: 650 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с открытыми разделами', async () => {
        await render(<EGRNAddressPurchaseFAQ />, {
            viewport: { width: 360, height: 2400 },
        });

        for (let _ of new Array(5)) { // eslint-disable-line
            await page.click(expanderSelector);
        }

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
