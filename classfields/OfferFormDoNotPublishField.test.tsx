import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormDoNotPublishField from './OfferFormDoNotPublishField';

it('на изменении отсылает стату', async() => {
    await renderComponent(
        (
            <OfferFormDoNotPublishField/>
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
            field: OfferFormFieldNames.DO_NOT_PUBLISH,
            event: 'click',
        },
    );
});
