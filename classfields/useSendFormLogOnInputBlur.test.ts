jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});
jest.mock('www-poffer/react/contexts/offerFormPage', () => {
    return {
        useOfferFormPageContext: jest.fn(),
    };
});

import { renderHook } from '@testing-library/react-hooks';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { useOfferFormPageContext } from 'www-poffer/react/contexts/offerFormPage';

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;
const useOfferFormContextMock = useOfferFormPageContext as jest.MockedFunction<typeof useOfferFormPageContext>;

import useSendFormLogOnInputBlur from './useSendFormLogOnInputBlur';

it('если поле заполнено неверно, отправит лог', async() => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        initialValues: {
            [FieldNames.MILEAGE]: 1,
        },
        validateFieldSilently: jest.fn(() => Promise.resolve({ type: FieldErrors.REQUIRED, text: 'Слишком маленький пробег' })),
        getFieldValue: jest.fn(() => 1000 as any),
    });
    useOfferFormContextMock.mockReturnValue(offerFormPageContextMock);

    const { result: sendFormLogOnInputBlur } = renderHook(() => useSendFormLogOnInputBlur());

    await sendFormLogOnInputBlur?.current(FieldNames.MILEAGE);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ event: 'error', field: FieldNames.MILEAGE });
});

describe('если поле заполнено верно', () => {
    it('отправит лог, если значение поменялось', async() => {
        useFormContextMock.mockReturnValue({
            ...coreFormContextMock,
            initialValues: {
                [FieldNames.MILEAGE]: 1,
            },
            validateFieldSilently: jest.fn(() => Promise.resolve(undefined)),
            getFieldValue: jest.fn(() => 1000 as any),
        });
        useOfferFormContextMock.mockReturnValue(offerFormPageContextMock);

        const { result: sendFormLogOnInputBlur } = renderHook(() => useSendFormLogOnInputBlur());

        await sendFormLogOnInputBlur?.current(FieldNames.MILEAGE);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ event: 'success', field: FieldNames.MILEAGE });
    });

    it('не отправит лог, если значение не поменялось', async() => {
        useFormContextMock.mockReturnValue({
            ...coreFormContextMock,
            initialValues: {
                [FieldNames.MILEAGE]: 1,
            },
            validateFieldSilently: jest.fn(() => Promise.resolve(undefined)),
            getFieldValue: jest.fn(() => 1 as any),
        });
        useOfferFormContextMock.mockReturnValue(offerFormPageContextMock);

        const { result: sendFormLogOnInputBlur } = renderHook(() => useSendFormLogOnInputBlur());

        await sendFormLogOnInputBlur?.current(FieldNames.MILEAGE);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });
});
