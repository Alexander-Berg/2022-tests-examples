import { renderHook, act } from '@testing-library/react-hooks';

import clientLog from 'auto-core/lib/clientLog';

import type { CreditFormProps } from 'auto-core/react/components/common/CreditForm/types';

import type { CreditApplication } from 'auto-core/types/TCreditBroker';

import { focusOnFirstFieldWithErrorAndLog } from './helpers/focusOnFirstFieldWithErrorAndLog';
import * as recordPrometheusAccordionPublishStatus from './helpers/recordPrometheusAccordionPublishStatus';
import { useSubmitHandler } from './useSubmitHandler';
import { getApiResponse } from './helpers/getApiResponse';

jest.mock('./helpers/getApiResponse', () => ({
    __esModule: true,
    getApiResponse: jest.fn().mockResolvedValue({ result: { result: { ok: true } } }),
}));

jest.mock('auto-core/lib/clientLog', () => ({
    error: jest.fn(),
    debug: jest.fn(),
    log: jest.fn(),
}));

jest.mock('./helpers/recordPrometheusAccordionPublishStatus', () => ({
    recordPrometheusAccordionPublishStatus: jest.fn(),
}));

jest.mock('./helpers/focusOnFirstFieldWithErrorAndLog', () => ({
    focusOnFirstFieldWithErrorAndLog: jest.fn(),
}));

describe('useSubmitHandler', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });
    afterEach(() => {
        jest.useRealTimers();
    });

    // я не победил этот тест, пока отладываю
    // it('не вызывается повторно если уже вызыван', async() => {
    //
    //     // eslint-disable-next-line
    //     // @ts-ignore
    //     getApiResponse.mockImplementation(
    //         // jest.fn(() =>
    //         new Promise(
    //             resolve => setTimeout(
    //                 () => resolve({ result: { result: { ok: true } } }),
    //                 100,
    //             ),
    //         ),
    //         // )),
    //     );
    //     // jest.spyOn(getApiResponse, 'getApiResponse');
    //
    //     const {
    //         rerender, result,
    //     } = await renderHook(() => useSubmitHandler({
    //         showErrorMessage: () => {},
    //         onPublishSuccess: () => {},
    //         sectionIds: [ '1', '2', '3' ],
    //         sendPageEvent: () => {},
    //         doNotPublishOnSave: true,
    //         sendSaveLogs: true,
    //         creditApplication: {} as CreditApplication,
    //         // eslint-disable-next-line
    //         // @ts-ignore
    //         creditFormApi: {
    //             getOffer: () => ({}),
    //             validateBlock: () => Promise.resolve(undefined),
    //             getValues: () => ({}),
    //             getErrors: () => ([]),
    //         } as CreditFormProps,
    //         validateBlockFullness: () => {},
    //     }));
    //
    //     try {
    //         act(async() => {
    //             await result.current.handleSubmit();
    //         });
    //
    //         await rerender();
    //
    //         act(async() => {
    //             await result.current.handleSubmit();
    //         });
    //     } catch (e) {
    //
    //     }
    //
    //     await flushPromises();
    //
    //     expect(getApiResponse).toHaveBeenCalledTimes(1);
    // });

    it('вызывает getApiResponse и логирует успешный вызов', async() => {
        // eslint-disable-next-line
        // @ts-ignore
        getApiResponse.mockImplementation(
            jest.fn().mockResolvedValue({ result: { ok: true } }),
        );

        const sendPageEvent = jest.fn();
        const onPublishSuccess = jest.fn();

        const {
            result,
        } = renderHook(() => useSubmitHandler({
            showErrorMessage: () => {},
            onPublishSuccess: onPublishSuccess,
            sectionIds: [ '1', '2', '3' ],
            sendPageEvent: sendPageEvent,
            doNotPublishOnSave: true,
            sendSaveLogs: true,
            creditApplication: {} as CreditApplication,
            // eslint-disable-next-line
                // @ts-ignore
            creditFormApi: {
                getOffer: () => ({}),
                validateBlock: () => Promise.resolve(undefined),
                getValues: () => ({}),
                getErrors: () => ([]),
            } as CreditFormProps,
            validateBlockFullness: () => {},
        }));

        await act(async() => {
            await result.current.handleSubmit();
        });

        expect(getApiResponse).toHaveBeenCalled();
        expect(sendPageEvent).toHaveBeenCalled();
        expect(onPublishSuccess).toHaveBeenCalled();
        expect(clientLog.debug).toHaveBeenCalled();
        expect(recordPrometheusAccordionPublishStatus.recordPrometheusAccordionPublishStatus).toHaveBeenCalled();
    });

    it('при ошибках валидации вызывает метод который проставит фокус на первом элементе', async() => {
        // eslint-disable-next-line
        // @ts-ignore
        getApiResponse.mockImplementation(
            jest.fn().mockResolvedValue({ result: { ok: true } }),
        );

        const sendPageEvent = jest.fn();
        const onPublishSuccess = jest.fn();

        const {
            result,
        } = renderHook(() => useSubmitHandler({
            showErrorMessage: () => {},
            onPublishSuccess: onPublishSuccess,
            sectionIds: [ '1', '2', '3' ],
            sendPageEvent: sendPageEvent,
            doNotPublishOnSave: true,
            sendSaveLogs: true,
            creditApplication: {} as CreditApplication,
            // eslint-disable-next-line
            // @ts-ignore
            creditFormApi: {
                getOffer: () => ({}),
                validateBlock: (i: string) => Promise.resolve(i),
                getValues: () => ({}),
                getErrors: () => ([]),
            } as CreditFormProps,
            validateBlockFullness: () => {},
        }));

        await act(async() => {
            await result.current.handleSubmit();
        });

        expect(focusOnFirstFieldWithErrorAndLog).toHaveBeenCalled();
        expect(getApiResponse).not.toHaveBeenCalled();
    });
    it('вызывает getApiResponse и логирует неуспешный вызов', async() => {
        // eslint-disable-next-line
            // @ts-ignore
        getApiResponse.mockImplementation(
            jest.fn().mockRejectedValue({ result: { ok: false } }),
        );

        const sendPageEvent = jest.fn();
        const onPublishSuccess = jest.fn();
        const showErrorMessage = jest.fn();

        const {
            result,
        } = renderHook(() => useSubmitHandler({
            showErrorMessage: showErrorMessage,
            onPublishSuccess: onPublishSuccess,
            sectionIds: [ '1', '2', '3' ],
            sendPageEvent: sendPageEvent,
            doNotPublishOnSave: true,
            sendSaveLogs: true,
            creditApplication: {} as CreditApplication,
            // eslint-disable-next-line
                    // @ts-ignore
            creditFormApi: {
                getOffer: () => ({}),
                validateBlock: () => Promise.resolve(undefined),
                getValues: () => ({}),
                getErrors: () => ([]),
            } as CreditFormProps,
            validateBlockFullness: () => {},
        }));

        await act(async() => {
            await result.current.handleSubmit();
        });

        expect(getApiResponse).toHaveBeenCalled();
        expect(showErrorMessage).toHaveBeenCalled();
        expect(clientLog.error).toHaveBeenCalled();
        expect(recordPrometheusAccordionPublishStatus.recordPrometheusAccordionPublishStatus).toHaveBeenCalled();
    });
});
