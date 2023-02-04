import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormPhotoReorderingField from './OfferFormPhotoReorderingField';

const photoMock = {
    name: 'photo1',
    preview: 'preview1',
    sizes: {
        '120x90': '//images.mds-proxy.test.avto.ru/get-autoru-all/1636323/32c63f642121a1977cce75903bac5a2f/small',
    },
};

it('при выборе чекбокса отсылается метрика', async() => {
    expect.assertions(1);

    const props = {
        initialValues: {
            [OfferFormFieldNames.PHOTO_REORDERING]: false,
            [OfferFormFieldNames.PHOTOS]: [ photoMock, photoMock ],
        },
    };

    await renderComponent(<OfferFormPhotoReorderingField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});

it('при отжатии чекбокса метрика не отсылается', async() => {
    expect.assertions(1);

    const props = {
        initialValues: {
            [OfferFormFieldNames.PHOTO_REORDERING]: true,
            [OfferFormFieldNames.PHOTOS]: [ photoMock, photoMock ],
        },
    };

    await renderComponent(<OfferFormPhotoReorderingField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
});
