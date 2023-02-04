import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import styles from '../styles.module.css';

import { AddItemSnippet } from '../';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

describe('AddItemSnippet', () => {
    describe(`Внешний вид`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <AddItemSnippet
                        title={'Добавить счётчик'}
                        onClick={() => {
                            return;
                        }}
                        withArrow
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    it('Ховер на десктопе', async () => {
        await render(
            <AddItemSnippet
                title={'Добавить счётчик'}
                onClick={() => {
                    return;
                }}
                withArrow
            />,
            renderOptions[0]
        );

        await page.hover(`.${styles.snippet}`);

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });
});
