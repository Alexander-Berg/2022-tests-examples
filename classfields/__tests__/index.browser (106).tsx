import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RangeControl } from '../';

const WrappedComponent = () => {
    const [values = {}, setState] = React.useState<{
        from?: number;
        to?: number;
    }>();

    return (
        <RangeControl
            name="range"
            fromName="from"
            toName="to"
            values={values}
            placeholders={['from', 'to']}
            onChange={(values) => setState(values)}
        />
    );
};

describe('RangeControl', () => {
    it('рисует контрол', async () => {
        await render(<WrappedComponent />, {
            viewport: { width: 350, height: 80 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus('.NumberRange__input:first-of-type input');
        await page.keyboard.type('20');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus('.NumberRange__input:last-of-type input');
        await page.keyboard.type('10');
        await page.$eval('.NumberRange__input:last-of-type input', (e: Element) => (e as HTMLInputElement).blur());
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus('.NumberRange__input:first-of-type input');
        await page.keyboard.type('1');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
