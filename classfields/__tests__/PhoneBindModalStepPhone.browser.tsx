import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PhoneBindError } from 'view/models';

import { PhoneBindModalStepPhone, IPhoneBindModalStepPhoneProps } from '../PhoneBindModalStepPhone';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const page: any;

const renderComponent = (onPhoneSubmit: IPhoneBindModalStepPhoneProps['onPhoneSubmit']) =>
    render(<PhoneBindModalStepPhone onPhoneSubmit={onPhoneSubmit} />, { viewport: { width: 310, height: 400 } });

const submitButtonSelector = '.PhoneBindModalStepPhone__submit-button';

const failedPromiseCreatorCreator = (err?: unknown) => () =>
    new Promise((resolve, reject) => setTimeout(reject, undefined, err)) as Promise<void>;
const infinitePromiseCreator = () => new Promise(() => undefined) as Promise<void>;

describe('PhoneBindModalStepPhone', () => {
    it('Базовое состояние', async () => {
        await renderComponent(() => new Promise((resolve) => resolve()));

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с введённым не полностью телефоном', async () => {
        await renderComponent(infinitePromiseCreator);

        await page.type('input', '799999');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с введённым телефоном', async () => {
        await renderComponent(infinitePromiseCreator);

        await page.type('input', '79999999999');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние загрузки', async () => {
        await renderComponent(infinitePromiseCreator);

        await page.type('input', '79999999999');
        await page.click(submitButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о том что телефон привязан', async () => {
        await renderComponent(failedPromiseCreatorCreator(PhoneBindError.PHONE_ALREADY_BOUND));

        await page.type('input', '79999999999');
        await page.click(submitButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о том что телефон имеет неправильный формат', async () => {
        await renderComponent(failedPromiseCreatorCreator(PhoneBindError.PHONE_BAD_NUM_FORMAT));

        await page.type('input', '79999999999');
        await page.click(submitButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с ошибкой о том что телефон забанен', async () => {
        await renderComponent(failedPromiseCreatorCreator(PhoneBindError.PHONE_BLOCKED));

        await page.type('input', '79999999999');
        await page.click(submitButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Состояние с неизвестной ошибкой', async () => {
        await renderComponent(failedPromiseCreatorCreator(PhoneBindError.UNKNOWN_ERROR));

        await page.type('input', '79999999999');
        await page.click(submitButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
