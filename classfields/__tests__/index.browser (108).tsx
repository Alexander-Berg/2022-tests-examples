import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SliderControl } from '../';

const className = 'slider';
const inputSelector = '.SliderInput2__input';

async function clearSliderInputAndType(text: string) {
    const inputValue = await page.$eval(inputSelector, (el) => (el as HTMLInputElement).value);

    await page.focus(inputSelector);
    await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

    return page.keyboard.type(text);
}

const WrappedComponent = (props: { initialValue: number }) => {
    const [values = { slider: props.initialValue }, setState] = React.useState<{ slider: number }>();

    return (
        <SliderControl
            className={className}
            name="slider"
            values={values}
            min={10}
            max={100}
            step={1}
            size="m"
            onChange={(values) => setState(values)}
        />
    );
};

describe('SliderControl', () => {
    it('рисует контрол', async () => {
        await render(<WrappedComponent initialValue={12} />, {
            viewport: { width: 200, height: 100 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await clearSliderInputAndType('25');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await clearSliderInputAndType('200');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await clearSliderInputAndType('5');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.$eval(inputSelector, (e: Element) => (e as HTMLInputElement).blur());
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
