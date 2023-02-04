/**
 * @jest-environment jsdom
 */

const mockSendFrontLogOnInputBlur = jest.fn();

jest.mock('www-poffer/react/hooks/useSendFormLogOnInputBlur', () => () => mockSendFrontLogOnInputBlur);

import React, { createRef } from 'react';
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormValidationResult, FormContext } from 'auto-core/react/components/common/Form/types';
import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormGosNumberField from './OfferFormGosNumberField';

it('на blur посылает стату', async() => {
    const { findByLabelText } = await await renderComponent(<OfferFormGosNumberField/>);
    const input = await findByLabelText(/госномер/i) as HTMLInputElement;

    userEvent.type(input, '{arrowleft}');
    userEvent.tab();

    expect(mockSendFrontLogOnInputBlur).toHaveBeenCalledWith(OfferFormFieldNames.GOS_NUMBER);
});

describe('валидация', () => {
    it('обязательно, если тачка зарегана в рашке', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
        };
        await renderComponent(<OfferFormGosNumberField/>, { formApi, initialValues });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.GOS_NUMBER);
        });

        expect(error?.type).toEqual(FieldErrors.REQUIRED);

        await act(async() => {
            formApi.current?.setFieldValue(OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA, true);
        });

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.GOS_NUMBER);
        });

        expect(error).toEqual(undefined);
    });

    it('не валидно, если не номер', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
            [OfferFormFieldNames.GOS_NUMBER]: 'asddddfer3',
        };
        await renderComponent(<OfferFormGosNumberField/>, { formApi, initialValues });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.GOS_NUMBER);
        });

        expect(error?.type).toEqual(FieldErrors.INCORRECT_VALUE);

        await act(async() => {
            formApi.current?.setFieldValue(OfferFormFieldNames.GOS_NUMBER, 'A 234 AA 77');
        });

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.GOS_NUMBER);
        });

        expect(error).toEqual(undefined);
    });
});

it('disabled для НЕ рашки', async() => {
    expect.assertions(1);
    const initialValues = {
        [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
    };
    const { findByLabelText } = await renderComponent(<OfferFormGosNumberField/>, { initialValues });
    const input = await findByLabelText(/госномер/i) as HTMLInputElement;

    expect(input.disabled).toEqual(true);
});

it('НЕ disabled для рашки', async() => {
    expect.assertions(1);
    const initialValues = {
        [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
    };
    const { findByLabelText } = await renderComponent(<OfferFormGosNumberField/>, { initialValues });
    const input = await findByLabelText(/госномер/i) as HTMLInputElement;

    expect(input.disabled).toEqual(false);
});
