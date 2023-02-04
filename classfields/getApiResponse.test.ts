import clientLog from 'auto-core/lib/clientLog';

import type { CreditFormProps } from 'auto-core/react/components/common/CreditForm/types';

import { recordPrometheusAccordionPublishStatus } from './recordPrometheusAccordionPublishStatus';
import { getApiResponse } from './getApiResponse';

jest.mock('./recordPrometheusAccordionPublishStatus', () => ({
    recordPrometheusAccordionPublishStatus: jest.fn(),
}));

jest.mock('auto-core/lib/clientLog', () => ({
    error: jest.fn(),
    debug: jest.fn(),
    log: jest.fn(),
}));

describe('getApiResponse', () => {
    it('вызовет сохранение заявки если doNotPublishOnSave = true', async() => {
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            saveCreditApplication: jest.fn(),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: true,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: () => {},
        });

        expect(creditFormApi.saveCreditApplication).toHaveBeenCalled();
    });

    it('вызовет публикаци заявки если doNotPublishOnSave = false', async() => {
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            publishCreditApplication: jest.fn(),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: false,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: () => {},
        });

        expect(creditFormApi.publishCreditApplication).toHaveBeenCalled();
    });

    it('выбросит исключение, если запрос не удастся', async() => {
        const sendPageEvent = jest.fn();
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            saveCreditApplication: jest.fn().mockResolvedValue(''),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: true,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: () => {},
            sendPageEvent: sendPageEvent,
        });

        expect(sendPageEvent).toHaveBeenCalledWith([ 'submit', 'publication-error', 'no-result' ]);
    });

    it('выбросит исключение, если запрос придет с ошибкой (поле error)', async() => {
        const sendPageEvent = jest.fn();
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            saveCreditApplication: jest.fn().mockResolvedValue({ error: [] }),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: true,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: () => {},
            sendPageEvent: sendPageEvent,
        });

        expect(sendPageEvent).toHaveBeenCalledWith([ 'submit', 'publication-error', 'action-error' ]);
    });

    it('выбросит исключение, если запрос придет с полем не правильным ok', async() => {
        const sendPageEvent = jest.fn();
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            saveCreditApplication: jest.fn().mockResolvedValue({ error: [] }),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: true,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: () => {},
            sendPageEvent: sendPageEvent,
        });

        expect(sendPageEvent).toHaveBeenCalledWith([ 'submit', 'publication-error', 'action-error' ]);
    });

    it('покажет сообщение об ошибке и залогирует', async() => {
        const sendPageEvent = jest.fn();
        const showErrorMessage = jest.fn();
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            saveCreditApplication: jest.fn().mockResolvedValue({ result: { ok: false } }),
            getErrors: jest.fn(),
        } as CreditFormProps;

        await getApiResponse({
            creditFormApi: creditFormApi,
            doNotPublishOnSave: true,
            filledFields: [ '1', '2', '3' ],
            showErrorMessage: showErrorMessage,
            sendPageEvent: sendPageEvent,
            sendSaveLogs: true,
        });

        expect(sendPageEvent).toHaveBeenCalledWith([ 'submit', 'publication-error', 'not-ok' ]);
        expect(showErrorMessage).toHaveBeenCalled();
        expect(clientLog.error).toHaveBeenCalled();
        expect(recordPrometheusAccordionPublishStatus).toHaveBeenCalled();
    });
});
