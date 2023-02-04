const mockSendFrontLogOnInputBlur = jest.fn();

jest.mock('www-poffer/react/hooks/useSendFormLogOnInputBlur', () => () => mockSendFrontLogOnInputBlur);

import React, { createRef } from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext, FormValidationResult } from 'auto-core/react/components/common/Form/types';
import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormUserEmailField
    from './OfferFormUserEmailField';

it('на blur посылает стату', async() => {
    expect.assertions(1);

    await renderComponent(<OfferFormUserEmailField/>);

    const input = await screen.getByRole('textbox');

    await act(async() => {
        userEvent.type(input, '{arrowleft}');
        userEvent.tab();
    });

    expect(mockSendFrontLogOnInputBlur).toHaveBeenCalledWith(OfferFormFieldNames.USEREMAIL);
});

it('валидация', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(<OfferFormUserEmailField/>, { formApi });

    let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

    await act(async() => {
        error = await formApi.current?.validateField(OfferFormFieldNames.USEREMAIL);
    });

    expect(error?.type).toEqual(FieldErrors.REQUIRED);

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.USEREMAIL, '123');
    });

    await act(async() => {
        error = await formApi.current?.validateField(OfferFormFieldNames.USEREMAIL);
    });

    expect(error?.type).toEqual(FieldErrors.INCORRECT_VALUE);

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.USEREMAIL, '123@pochto.ru');
    });

    await act(async() => {
        error = await formApi.current?.validateField(OfferFormFieldNames.USEREMAIL);
    });

    expect(error?.type).toEqual(undefined);
});
