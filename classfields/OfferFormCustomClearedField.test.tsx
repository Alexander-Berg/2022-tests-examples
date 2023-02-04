import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';

import '@testing-library/jest-dom';
import { PtsStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import OfferFormCustomClearedField from './OfferFormCustomClearedField';

it('при смене типа ПТС на "без ПТС" выставит значение в true', async() => {
    const initialValues = {
        [OfferFormFieldNames.CUSTOM_NOT_CLEARED]: false,
        [OfferFormFieldNames.PTS_STATUS]: PtsStatus.ORIGINAL,
    };
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { findByLabelText } = await renderComponent(<OfferFormCustomClearedField/>, { initialValues, formApi });

    const checkbox = await findByLabelText(/Не растаможен/i) as HTMLInputElement;
    expect(checkbox.checked).toBe(false);

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.PTS_STATUS, PtsStatus.NO_PTS);
    });
    expect(checkbox.checked).toBe(true);
});

describe('при смене ПТС на не "без ПТС"', () => {
    it('если инпут скрыт сбросит значение на false', async() => {
        const initialValues = {
            [OfferFormFieldNames.CUSTOM_NOT_CLEARED]: true,
            [OfferFormFieldNames.PTS_STATUS]: PtsStatus.ORIGINAL,
        };
        const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const { findByLabelText } = await renderComponent(<OfferFormCustomClearedField isHidden={ true }/>, { initialValues, formApi });

        await act(async() => {
            formApi.current?.setFieldValue(OfferFormFieldNames.PTS_STATUS, PtsStatus.DUPLICATE);
        });

        const checkbox = await findByLabelText(/Не растаможен/i) as HTMLInputElement;
        expect(checkbox.checked).toBe(false);
    });

    it('если инпут не скрыт не будет трогать значение', async() => {
        const initialValues = {
            [OfferFormFieldNames.CUSTOM_NOT_CLEARED]: true,
            [OfferFormFieldNames.PTS_STATUS]: PtsStatus.ORIGINAL,
        };
        const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const { findByLabelText } = await renderComponent(<OfferFormCustomClearedField/>, { initialValues, formApi });

        await act(async() => {
            formApi.current?.setFieldValue(OfferFormFieldNames.PTS_STATUS, PtsStatus.DUPLICATE);
        });

        const checkbox = await findByLabelText(/Не растаможен/i) as HTMLInputElement;
        expect(checkbox.checked).toBe(true);
    });
});

it('при клике на чекбокс отсылается метрика', async() => {

    const { findByLabelText } = await renderComponent(<OfferFormCustomClearedField/>);

    const checkbox = await findByLabelText(/Не растаможен/i) as HTMLInputElement;

    userEvent.click(checkbox);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ event: 'click', field: OfferFormFieldNames.CUSTOM_NOT_CLEARED });
});
