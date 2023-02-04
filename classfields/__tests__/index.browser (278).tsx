import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PerformStageErrorType } from 'realty-core/types/payment/purchase';

import { PaymentModalError } from '../PaymentModalError';
import { ErrorType } from '../PaymentModalError.types';

const smallDimension = { viewport: { width: 320, height: 400 } };
const mediumDimension = { viewport: { width: 370, height: 400 } };
const wideDimenstion = { viewport: { width: 660, height: 400 } };

const dimensions = [smallDimension, mediumDimension, wideDimenstion];

const renderSeveralResolutions = async (Component: React.ReactElement, dimensions: typeof smallDimension[]) => {
    for (const dimension of dimensions) {
        await render(Component, dimension);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('PaymentModalError', () => {
    it('ошибка соединения', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={ErrorType.CONNECTION} />,
            dimensions
        );
    });

    it('ошибка инициализации', async () => {
        await renderSeveralResolutions(<PaymentModalError onClick={noop} errorType={ErrorType.INIT} />, dimensions);
    });

    it('ошибка оплаты - по умолчанию', async () => {
        await renderSeveralResolutions(<PaymentModalError onClick={noop} errorType={ErrorType.PAYMENT} />, dimensions);
    });

    it('ошибка оплаты - INTERNAL_ERROR', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.INTERNAL_ERROR} />,
            dimensions
        );
    });

    it('ошибка оплаты - CARD_EXPIRED', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.CARD_EXPIRED} />,
            dimensions
        );
    });

    it('ошибка оплаты - INVALID_CARD_CREDENTIALS', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.INVALID_CARD_CREDENTIALS} />,
            dimensions
        );
    });

    it('ошибка оплаты - NO_ENOUGH_FUNDS', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.NO_ENOUGH_FUNDS} />,
            dimensions
        );
    });

    it('ошибка оплаты - INVALID_CARD_NUMBER', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.INVALID_CARD_NUMBER} />,
            dimensions
        );
    });

    it('ошибка оплаты - INVALID_CARD_CSC', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={PerformStageErrorType.INVALID_CARD_CSC} />,
            dimensions
        );
    });

    it('покупка невозможна', async () => {
        await renderSeveralResolutions(
            <PaymentModalError onClick={noop} errorType={ErrorType.UNAVAILABLE} />,
            dimensions
        );
    });

    it('таймаут', async () => {
        await renderSeveralResolutions(<PaymentModalError errorType={ErrorType.TIMEOUT} />, dimensions);
    });
});
