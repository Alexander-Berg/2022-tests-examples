import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';

import OfferFormPtsStatusField from './OfferFormPtsStatusField';

it('при выборе Типа документа отсылается метрика', async() => {

    const { getAllByRole } = await renderComponent(<OfferFormPtsStatusField/>);

    const tags = getAllByRole('button');

    userEvent.click(tags[0]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});
