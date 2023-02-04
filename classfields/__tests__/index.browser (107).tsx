import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SelectControl } from '../';

const className = 'select';

const getSelectMenuItem = (n: number) => `.Popup_visible .Menu .Menu__item:nth-of-type(${n})`;

const WrappedComponent = (props: { initialValue?: string | string[]; multiple?: boolean; placeholder?: string }) => {
    const [values = { select: props.initialValue }, setState] = React.useState<{ select?: string | string[] }>();

    return (
        <SelectControl
            className={className}
            name="select"
            values={values}
            size="m"
            options={[
                { value: '1', label: 'One' },
                { value: '2', label: 'Two' },
                { value: '3', label: 'Three' },
                { value: '4', label: 'Four' },
                { value: '5', label: 'Five' },
            ]}
            onChange={(values) => setState(values)}
            placeholder={props.placeholder}
            multiple={props.multiple}
        />
    );
};

describe('SelectControl', () => {
    it('рисует обычный селект', async () => {
        await render(<WrappedComponent initialValue="2" />, {
            viewport: { width: 200, height: 300 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        await page.click(`.${className}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(getSelectMenuItem(4));
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует мультиселект', async () => {
        await render(<WrappedComponent placeholder="Select" multiple />, {
            viewport: { width: 200, height: 300 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        await page.click(`.${className}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(getSelectMenuItem(1));
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(getSelectMenuItem(3));
        await page.click(getSelectMenuItem(5));
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click('body');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
