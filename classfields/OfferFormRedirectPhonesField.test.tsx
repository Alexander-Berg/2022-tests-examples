import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormRedirectPhonesField from './OfferFormRedirectPhonesField';

it('на изменении отсылает стату', async() => {
    await renderComponent(
        (
            <OfferFormRedirectPhonesField/>
        ),
        {
            offerFormContext: {
                ...offerFormPageContextMock,
            },
        },
    );

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith(
        {
            field: OfferFormFieldNames.REDIRECT_PHONES,
            event: 'click',
        },
    );
});
