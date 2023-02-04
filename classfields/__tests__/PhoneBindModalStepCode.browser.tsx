import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PhoneBindModalStepCode, IPhoneBindModalStepCodeProps } from '../PhoneBindModalStepCode';

import * as mock from './PhoneBindModalStepCode.mocks';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const page: any;

const renderComponent = (props: IPhoneBindModalStepCodeProps) =>
    render(<PhoneBindModalStepCode {...props} />, { viewport: { width: 310, height: 400 } });

const resendButtonSelector = '.PhoneBindModalStepCode__resend-button';

describe('PhoneBindModalStepCode', () => {
    it('Базовое состояние', async () => {
        await renderComponent(mock.defaultState);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Базовое состояние с таймером', async () => {
        await renderComponent(mock.defaultWithTimer);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с не до конца введённым кодом', async () => {
        await renderComponent(mock.defaultState);

        await page.type('input', '123');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние загрузки (submit)', async () => {
        await renderComponent(mock.loadingSubmit);

        await page.type('input', '12345');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние загрузки (resend)', async () => {
        await renderComponent(mock.loadingResend);

        await page.click(resendButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с неизвестной ошибкой (submit)', async () => {
        await renderComponent(mock.errorSubmitUnknown);

        await page.type('input', '12345');

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о неправильном коде (submit)', async () => {
        await renderComponent(mock.errorSubmitWrongCode);

        await page.type('input', '12345');

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с неизвестной ошибкой (resend)', async () => {
        await renderComponent(mock.errorResendUnknown);

        await page.click(resendButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о неправильном формате телефона (resend)', async () => {
        await renderComponent(mock.errorResendPhoneFormat);

        await page.click(resendButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о заблокированном телефоне (resend)', async () => {
        await renderComponent(mock.errorResendPhoneBlocked);

        await page.click(resendButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о уже привязанном телефоне (resend)', async () => {
        await renderComponent(mock.errorResendPhoneBound);

        await page.click(resendButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
