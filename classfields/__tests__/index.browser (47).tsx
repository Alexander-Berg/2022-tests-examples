import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { NumberInput } from '../index';

const renderOptions = { viewport: { width: 400, height: 120 } };

const LogicComponent = () => {
    const [value, setState] = React.useState<number>();

    const eventHandler = (value: number | undefined) => setState(value);

    return <NumberInput variant="bordered" size="l" label="Число" value={value} onChange={eventHandler} />;
};

describe('NumberInput', () => {
    it('Внешний вид', async () => {
        await render(<LogicComponent />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
