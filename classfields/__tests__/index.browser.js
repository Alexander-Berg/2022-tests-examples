import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Accordion } from '../';
import styles from '../AccordionEntry/styles.module.css';

const OPTIONS = {
    viewport: { width: 300, height: 500 }
};

const entries = [
    {
        id: 'a',
        title: 'Pooch',
        content: (
            <div>
                <p>123</p>
                <p>456</p>
            </div>
        )
    },
    { id: 'b', title: 'Catdog', content: 'content\n\n\ncontent' },
    { id: 'c', title: <s>Bartolomew</s>, content: 'content' }
];

const firstExpanderSelector = `.${styles.header}:first-of-type`;

describe('Accordion', () => {
    it('должен рендериться с изначально открытым первым разделом', async() => {
        await render(<Accordion entries={entries} initialExpandedEntryIds={[ 'a' ]} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('должен открывать закрытый раздел (первый) по клику', async() => {
        await render(<Accordion entries={entries} />, OPTIONS);

        await page.click(firstExpanderSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('должен закрывать открытый раздел (первый) по клику', async() => {
        await render(<Accordion entries={entries} initialExpandedEntryIds={[ 'a' ]} />, OPTIONS);

        await page.click(firstExpanderSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('должна рендериться мобильная версия', async() => {
        await render(<Accordion platform='mobile' entries={entries} initialExpandedEntryIds={[ 'a' ]} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
