import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CheckboxControl } from '../';

const WrappedComponent = (props: { checkedValue: string | string[]; initialValue?: string | string[] }) => {
    const [values = { checkbox: props.initialValue }, setState] = React.useState<{ checkbox?: string | string[] }>();

    return (
        <CheckboxControl
            name="checkbox"
            values={values}
            label="Checkbox"
            onChange={(values) => setState(values)}
            {...props}
        />
    );
};

describe('CheckboxControl', () => {
    it('рисует контрол c единичным значением', async () => {
        await render(<WrappedComponent checkedValue="1" />, {
            viewport: { width: 180, height: 80 },
        });

        await page.click(`[data-test=checkbox]`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`[data-test=checkbox]`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует контрол c множественным значением', async () => {
        await render(<WrappedComponent checkedValue={['1', '2']} initialValue={['2', '1']} />, {
            viewport: { width: 180, height: 80 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`[data-test=checkbox]`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
