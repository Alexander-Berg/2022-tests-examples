import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RadioGroupControl } from '../';

const WrappedComponent = (props: { initialValue?: string }) => {
    const [values = { radioGroup: props.initialValue }, setState] = React.useState<{ radioGroup?: string }>();

    return (
        <RadioGroupControl
            name="radioGroup"
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

describe('RadioGroupControl', () => {
    it('рисует контрол', async () => {
        await render(<WrappedComponent initialValue="2" />, {
            viewport: { width: 350, height: 80 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        await page.click(`.Radio:nth-child(2)`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.Radio:nth-child(1)`);
        await page.click(`.Radio:nth-child(3)`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
