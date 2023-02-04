import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { buttonsTests } from './buttons.browser';

import { storeAllSuccess, storeAllInProgress, storeAllInvalid, storeErrors, storeNoChecks } from './stub';

import { renderOptions, Component } from './common';

describe('ManagerUserChecks', () => {
    describe('Внешний вид', () => {
        it('Все проверки готовы и пройдены успешно', async () => {
            await render(<Component store={storeAllSuccess} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Проверки ожидаются', async () => {
            await render(<Component store={storeAllInProgress} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Все проверки готовы и провалены', async () => {
            await render(<Component store={storeAllInvalid} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('При прохождении проверок произошли ошибки', async () => {
            await render(<Component store={storeErrors} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Не пришли данные по проверкам', async () => {
            await render(<Component store={storeNoChecks} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Показ дополнительных кнопок', () => buttonsTests);
});
