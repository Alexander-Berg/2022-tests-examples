import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CheckboxGroupControl } from '../';

const WrappedComponent = (props: { initialValue?: string[] }) => {
    const [values = { checkboxGroup: props.initialValue }, setState] = React.useState<{ checkboxGroup?: string[] }>();

    return (
        <CheckboxGroupControl
            name="checkboxGroup"
            values={values}
            options={[
                { value: '1', label: 'One' },
                { value: '2', label: 'Two' },
                { value: '3', label: 'Three' },
            ]}
            onChange={(values) => setState(values)}
        />
    );
};

describe('CheckboxGroupControl', () => {
    it('рисует контрол', async () => {
        await render(<WrappedComponent initialValue={['1']} />, {
            viewport: { width: 350, height: 80 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        await page.click(`[data-test=checkbox]:nth-child(1)`);
        await page.click(`[data-test=checkbox]:nth-child(2)`);
        await page.click(`[data-test=checkbox]:nth-child(3)`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
