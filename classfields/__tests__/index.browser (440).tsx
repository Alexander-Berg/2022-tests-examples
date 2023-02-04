import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageFilters, IMortgageFiltersProps } from '../';

import { getSliderValues, getSliderLimits, getBanks, getPageParams } from './mocks';

const Component = (props: Partial<IMortgageFiltersProps>): React.ReactElement => (
    <div style={{ backgroundColor: '#f7f7f6', padding: '8px' }}>
        <MortgageFilters
            values={getSliderValues()}
            defaultValues={getSliderValues()}
            limits={getSliderLimits()}
            defaultLimits={getSliderLimits()}
            queryId="123"
            banks={getBanks()}
            count={45}
            {...props}
        />
    </div>
);

const EXPAND_BUTTON_SELECTOR = '.Button_view_transparent-blue';

describe('MortgageFilters', () => {
    it('Рисует пустой закрытый фильтр', async () => {
        await render(<Component />, { viewport: { width: 1000, height: 280 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует пустой открытый фильтр', async () => {
        await render(<Component />, { viewport: { width: 1000, height: 800 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный закрытый фильтр', async () => {
        await render(<Component values={getPageParams()} />, { viewport: { width: 1000, height: 280 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный открытый фильтр', async () => {
        await render(<Component values={getPageParams()} />, { viewport: { width: 1000, height: 800 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует фильтр с измененными контролами слайдера', async () => {
        await render(<Component values={getSliderValues(2)} />, { viewport: { width: 1000, height: 280 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует фильтр с измененными ценой и взносом', async () => {
        await render(<Component values={getSliderValues()} />, { viewport: { width: 1000, height: 280 } });

        const inputSelector = `.SliderInput2:first-child .SliderInput2__input`;
        const inputValue = await page.$eval(inputSelector, (el) => (el as HTMLInputElement).value);

        await page.focus(inputSelector);
        await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

        await page.keyboard.type('12501000');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
