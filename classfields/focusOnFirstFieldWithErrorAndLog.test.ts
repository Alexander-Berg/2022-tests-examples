import clientLog from 'auto-core/lib/clientLog';

import type { CreditFormProps } from 'auto-core/react/components/common/CreditForm/types';

import type { CreditApplication } from 'auto-core/types/TCreditBroker';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import { focusOnFirstFieldWithErrorAndLog } from './focusOnFirstFieldWithErrorAndLog';
import { recordPrometheusAccordionPublishStatus } from './recordPrometheusAccordionPublishStatus';

jest.mock('./recordPrometheusAccordionPublishStatus', () => ({
    recordPrometheusAccordionPublishStatus: jest.fn(),
}));

jest.mock('auto-core/lib/clientLog', () => ({
    error: jest.fn(),
    debug: jest.fn(),
    log: jest.fn(),
}));

describe('focusOnFirstFieldWithErrorAndLog', () => {
    const sendPageEvent = jest.fn();

    it('фокусируется на поле и логирует, отправляет событие validation error', () => {
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            focusField: jest.fn(),
            getErrors: () => ({ '1': '1', '2': '2', '3': '3' }),
            getBlockFieldNames: () => [ '1', '2', '3' ],
        } as CreditFormProps;

        focusOnFirstFieldWithErrorAndLog({
            filteredSectionIDs: [ '1', '2', '3' ],
            firstErrorBlockIndex: 0,
            creditFormApi: creditFormApi,
            sendPageEvent: sendPageEvent,
            creditApplication: {} as CreditApplication,
            sendSaveLogs: true,
            filledFields: [ '1', '2', '3' ],
        });

        expect(creditFormApi.focusField).toHaveBeenCalledWith('1');
        expect(clientLog.error).toHaveBeenCalled();
        expect(recordPrometheusAccordionPublishStatus).toHaveBeenCalled();
        expect(sendPageEvent).toHaveBeenCalledWith([
            'submit', 'validation-error',
        ]);
    });

    it('фокусируется на поле и логирует, отправляет событие agreement-error', () => {
        // eslint-disable-next-line
        // @ts-ignore
        const creditFormApi = {
            focusField: jest.fn(),
            getErrors: () => ({ '1': '1', '2': '2', '3': '3', [CreditFormFieldType.OKB_STATEMENT_AGREEMENT]: '2' }),
            getBlockFieldNames: () => [ '1', '2', '3' ],
        } as CreditFormProps;

        focusOnFirstFieldWithErrorAndLog({
            filteredSectionIDs: [ '1', '2', '3' ],
            firstErrorBlockIndex: 0,
            creditFormApi: creditFormApi,
            sendPageEvent: sendPageEvent,
            creditApplication: {} as CreditApplication,
            sendSaveLogs: true,
            filledFields: [ '1', '2', '3' ],
        });

        expect(creditFormApi.focusField).toHaveBeenCalledWith('1');
        expect(clientLog.error).toHaveBeenCalled();
        expect(recordPrometheusAccordionPublishStatus).toHaveBeenCalled();
        expect(sendPageEvent).toHaveBeenCalledWith([
            'submit', 'agreement-error',
        ]);
    });
});
