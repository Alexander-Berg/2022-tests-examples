import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FormReadonlyField } from '../index';

const renderOptions = { viewport: { width: 400, height: 120 } };

describe('FormReadonlyField', () => {
    describe('Внешний вид', () => {
        it('Текст', async () => {
            await render(
                <FormReadonlyField size="l" label={'Адрес'} type="text">
                    Улица Пушкина, дом Колотушкина, 16
                </FormReadonlyField>,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Телефон', async () => {
            await render(
                <FormReadonlyField size="l" label={'Телефон'} type="tel">
                    +79992134916
                </FormReadonlyField>,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Дата', async () => {
            await render(
                <FormReadonlyField size="l" label={'Дата'} type="date">
                    2021-02-24T07:57:41.122Z
                </FormReadonlyField>,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
