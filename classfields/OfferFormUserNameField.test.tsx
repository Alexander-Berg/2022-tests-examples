const mockSendFrontLogOnInputBlur = jest.fn();

jest.mock('www-poffer/react/hooks/useSendFormLogOnInputBlur', () => () => mockSendFrontLogOnInputBlur);

import React, { createRef } from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormValidationResult, FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormUserNameField
    from './OfferFormUserNameField';

it('на blur посылает стату', async() => {
    await renderComponent(<OfferFormUserNameField/>);

    const input = await screen.getByRole('textbox');

    await act(async() => {
        userEvent.type(input, '{arrowleft}');
        userEvent.tab();
    });

    expect(mockSendFrontLogOnInputBlur).toHaveBeenCalledWith(OfferFormFieldNames.USERNAME);
});

it('поле необязательно', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    await renderComponent(<OfferFormUserNameField/>, { formApi });

    let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

    await act(async() => {
        error = await formApi.current?.validateField(OfferFormFieldNames.USERNAME);
    });

    // Раньше поле было обязательным к заполнению, но теперь это не так
    // поэтому больше не ожидаем ошибок, если поле пустое. Подробнее в тикете: https://st.yandex-team.ru/AUTORUFRONT-22219
    expect(error?.type).toEqual(undefined);

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.USERNAME, '123');
    });

    await act(async() => {
        error = await formApi.current?.validateField(OfferFormFieldNames.USERNAME);
    });

    expect(error?.type).toEqual(undefined);
});
