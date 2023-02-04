import React from 'react';

import { render } from 'jest-puppeteer-react';

import noop from 'lodash';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import buttonsStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';

import checksStyles from '../styles.module.css';

import {
    storeExtremist,
    storeExtremistGate,
    storeWantedError,
    storeWantedErrorGate,
    storePassportInvalid,
    storePassportInvalidGate,
    storeMultipleErrors,
} from './stub';

const selectors = {
    checks: {
        itemN: (n: number) => `.${checksStyles.checkResults}:nth-child(${n}) span`,
    },
    spanButton: 'span[role=button]',
    confirmModalButton: `.${buttonsStyles.buttons} button`,
};

import { renderOptions, Component } from './common';

// eslint-disable-next-line jest/no-export
export const buttonsTests = describe('Показ дополнительных кнопок', () => {
    const Gate = {
        create: (path: string) => {
            switch (path) {
                case 'manager.get_user_spectrum_report': {
                    return Promise.resolve({
                        ...storeExtremistGate,
                    });
                }
                case 'manager.repeat_user_check': {
                    return Promise.resolve({
                        ...storeWantedErrorGate.managerUser,
                    });
                }
            }
        },
    };

    const GatePending = {
        create: () => {
            return new Promise(noop);
        },
    };

    const GateModalSuccess = {
        create: (path: string) => {
            switch (path) {
                case 'manager.repeat_user_check': {
                    return Promise.resolve({
                        ...storePassportInvalidGate.managerUser,
                    });
                }
            }
        },
    };

    const GateFail = {
        create: () => {
            return Promise.reject();
        },
    };

    describe('Кнопка подробнее', () => {
        it('Успешно получили данные', async () => {
            await render(<Component store={storeExtremist} Gate={Gate} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.checks.itemN(5));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Ожидание', async () => {
            await render(<Component store={storeExtremist} Gate={GatePending} />, renderOptions[1]);

            await page.click(selectors.checks.itemN(5));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Упала ручка получения отчета', async () => {
            await render(<Component store={storeExtremist} Gate={GateFail} />, renderOptions[1]);

            await page.click(selectors.checks.itemN(5));

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Кнопка повторить', () => {
        it('Успешно повторить', async () => {
            await render(<Component store={storeWantedError} Gate={Gate} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.spanButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Ожидание', async () => {
            await render(<Component store={storeWantedError} Gate={GatePending} />, renderOptions[1]);

            await page.click(selectors.spanButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Упала ручка повтора проверок', async () => {
            await render(<Component store={storeWantedError} Gate={GateFail} />, renderOptions[1]);

            await page.click(selectors.spanButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Кнопка утвердить', () => {
        it('Внешний вид', async () => {
            await render(<Component store={storePassportInvalid} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Успешно утверждено', async () => {
            await render(<Component store={storePassportInvalid} Gate={GateModalSuccess} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.spanButton);

            await page.click(selectors.confirmModalButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Упала ручка повтора проверки', async () => {
            await render(<Component store={storePassportInvalid} Gate={GateFail} />, renderOptions[1]);

            await page.click(selectors.spanButton);

            await page.click(selectors.confirmModalButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        renderOptions.forEach((renderOption) => {
            it(`Внешний вид модалки ${renderOption.viewport.width}px`, async () => {
                await render(<Component store={storePassportInvalid} Gate={GateModalSuccess} />, renderOption);

                await page.click(selectors.spanButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Ожидание модалки ${renderOption.viewport.width}px`, async () => {
                await render(<Component store={storePassportInvalid} Gate={GatePending} />, renderOption);

                await page.click(selectors.spanButton);

                await page.click(selectors.confirmModalButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('При клике на одну кнопку другие блокируются', () => {
        it('Ожидание', async () => {
            await render(<Component store={storeMultipleErrors} Gate={GatePending} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.spanButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
