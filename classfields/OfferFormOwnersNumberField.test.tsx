import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';

import '@testing-library/jest-dom';
import { PtsStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import type { FormContext } from 'auto-core/react/components/common/Form/types';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import OfferFormOwnersNumberField from './OfferFormOwnersNumberField';

it('при выборе владельца отсылается метрика', async() => {

    const { getAllByRole } = await renderComponent(<OfferFormOwnersNumberField/>);

    const tags = getAllByRole('button');

    userEvent.click(tags[0]);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);

    userEvent.click(tags[2]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
});

it('при смене типа ПТС на "без ПТС" сбросит значение в поле', async() => {
    const initialValues = {
        [OfferFormFieldNames.OWNERS_NUMBER]: 1,
        [OfferFormFieldNames.PTS_STATUS]: PtsStatus.ORIGINAL,
    };
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(<OfferFormOwnersNumberField/>, { initialValues, formApi });
    let selectedOption = document.querySelector('.Button_checked')?.textContent;

    expect(selectedOption).toBe('Первый');

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.PTS_STATUS, PtsStatus.NO_PTS);
    });

    selectedOption = document.querySelector('.Button_checked')?.textContent;
    expect(selectedOption).toBe(undefined);
});
