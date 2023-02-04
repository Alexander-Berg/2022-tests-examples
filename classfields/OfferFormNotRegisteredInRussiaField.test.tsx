import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormNotRegisteredInRussiaField
    from './OfferFormNotRegisteredInRussiaField';

it('на click посылает стату', async() => {
    const { findByLabelText } = await renderComponent(<OfferFormNotRegisteredInRussiaField/>);
    const checkbox = await findByLabelText('Автомобиль не на учёте в РФ') as HTMLInputElement;

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA, event: 'click' });
});
