import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageFilters, IMortgageFiltersProps } from '../';

import styles from './styles.module.css';
import { getSliderValues, getSliderLimits, getBanks, getPageParams } from './mocks';

const Component = (props: Partial<IMortgageFiltersProps>): React.ReactElement => (
    <MortgageFilters
        values={getSliderValues()}
        defaultValues={getSliderValues()}
        limits={getSliderLimits()}
        defaultLimits={getSliderLimits()}
        queryId="123"
        banks={getBanks()}
        count={45}
        className={styles.filters}
        {...props}
    />
);

const EXPAND_BUTTON_SELECTOR = 'div[class^="MortgageFilters__expandButton"]';
const MORTGAGE_TYPE_SELECTOR = '.Select:nth-child(2)';

describe('MortgageFilters', () => {
    it('Рисует пустой закрытый фильтр', async () => {
        await render(<Component />, { viewport: { width: 360, height: 480 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует пустой открытый фильтр', async () => {
        await render(<Component />, { viewport: { width: 360, height: 1500 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует пустой открытый фильтр (плавающий блок сабмита)', async () => {
        await render(<Component />, { viewport: { width: 360, height: 800 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный закрытый фильтр', async () => {
        await render(<Component values={getPageParams()} />, { viewport: { width: 360, height: 480 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный открытый фильтр', async () => {
        await render(<Component values={getPageParams()} />, { viewport: { width: 360, height: 1500 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует фильтр с открытым селектом', async () => {
        await render(<Component />, { viewport: { width: 360, height: 480 } });

        await page.click(MORTGAGE_TYPE_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует фильтр с измененными ценой и взносом', async () => {
        await render(<Component values={getSliderValues()} />, { viewport: { width: 360, height: 580 } });

        const inputSelector = `.SliderInput2 .SliderInput2__input`;
        const inputValue = await page.$eval(inputSelector, (el) => (el as HTMLInputElement).value);

        await page.focus(inputSelector);
        await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

        await page.keyboard.type('12501000');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует фильтр с паранжой', async () => {
        await render(<Component values={getSliderValues()} withParanja />, { viewport: { width: 360, height: 1400 } });

        await page.click(EXPAND_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует всегда открытый фильтр', async () => {
        await render(<Component values={getSliderValues()} alwaysExpanded />, {
            viewport: { width: 360, height: 1400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
