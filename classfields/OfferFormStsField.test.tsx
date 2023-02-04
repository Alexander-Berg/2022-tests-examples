const mockSendFrontLogOnInputBlur = jest.fn();

jest.mock('www-poffer/react/hooks/useSendFormLogOnInputBlur', () => () => mockSendFrontLogOnInputBlur);

import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormStsField
    from './OfferFormStsField';

it('на blur посылает стату', async() => {
    expect.assertions(1);

    const { findByLabelText } = await renderComponent(<OfferFormStsField/>);
    const input = await findByLabelText(/стс/i) as HTMLInputElement;

    userEvent.type(input, '{arrowleft}');
    userEvent.tab();

    expect(mockSendFrontLogOnInputBlur).toHaveBeenCalledWith(OfferFormFieldNames.STS);
});

it('disabled для НЕ рашки', async() => {
    expect.assertions(1);

    const initialValues = {
        [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
    };
    const { findByLabelText } = await renderComponent(<OfferFormStsField/>, { initialValues });
    const input = await findByLabelText(/стс/i) as HTMLInputElement;

    expect(input.disabled).toEqual(true);
});

it('НЕ disabled для рашки', async() => {
    expect.assertions(1);
    const initialValues = {
        [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
    };
    const { findByLabelText } = await renderComponent(<OfferFormStsField/>, { initialValues });
    const input = await findByLabelText(/стс/i) as HTMLInputElement;

    expect(input.disabled).toEqual(false);
});
