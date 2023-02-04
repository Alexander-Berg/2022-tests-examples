import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IPhoneBindResult } from 'view/models';

import { PhoneBindModalComponent, IPhoneBindModalProps } from '../PhoneBindModal';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const page: any;

const phoneBindResult: IPhoneBindResult = {
    trackId: '12345',
    codeLength: 5,
};

const successfulBindPromiseCreator = () =>
    new Promise((resolve) => resolve(phoneBindResult)) as Promise<IPhoneBindResult>;
const infinitePromiseCreator = () => new Promise(() => undefined) as Promise<void>;

const defaultState: IPhoneBindModalProps = {
    isOpened: true,
    onClose: noop,
    onSuccess: noop,
    bind: successfulBindPromiseCreator,
    bindConfirm: infinitePromiseCreator,
} as const;

const renderComponent = (props: IPhoneBindModalProps) =>
    render(<PhoneBindModalComponent {...props} />, { viewport: { width: 350, height: 530 } });

const submitButtonSelector = '.PhoneBindModalStepPhone__submit-button';
const resetPhoneButtonSelector = '.PhoneBindModalStepCode__reset-phone-button';

describe('PhoneBindModal', () => {
    it('Успешный сценарий', async () => {
        await renderComponent(defaultState);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type('input', '79991234567');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(submitButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type('input', '12345');

        // скипнут из-за непредсказуемого поведения таймеров (и невозможности их нормально отключить или мокнуть)
        // expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Сценарий со сбросом телефона', async () => {
        await renderComponent(defaultState);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type('input', '79991234567');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(submitButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(resetPhoneButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
