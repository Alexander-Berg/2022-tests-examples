import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormWithoutPhotosField from './OfferFormWithoutPhotosField';

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
            [OfferFormFieldNames.WITHOUT_PHOTOS]: false,
        },
    };

    await renderComponent(<OfferFormWithoutPhotosField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});

it('при отжатии чекбокса метрика не отсылается', async() => {
    expect.assertions(1);

    const props = {
        initialValues: {
            [OfferFormFieldNames.WITHOUT_PHOTOS]: true,
        },
    };

    await renderComponent(<OfferFormWithoutPhotosField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
});

it('при наведении на тултип показываем нужный текст', async() => {
    const props = {
        initialValues: {
            [OfferFormFieldNames.WITHOUT_PHOTOS]: true,
        },
    };

    const { getByText } = await renderComponent(<OfferFormWithoutPhotosField/>, props);

    const tooltip = getByText(/\+70% к просмотрам/);

    userEvent.hover(tooltip);

    await waitFor(() => {
        const tooltipContent = document.querySelector('.OfferFormWithoutPhotosField__tooltipContent');
        expect(tooltipContent).toBeInTheDocument();
    });
});

it('если есть фото, то мы не показываем данный чекбокс', async() => {
    const props = {
        initialValues: {
            [OfferFormFieldNames.PHOTOS]: [ photoMock ],
            [OfferFormFieldNames.WITHOUT_PHOTOS]: true,
        },
    };

    const { queryByRole } = await renderComponent(<OfferFormWithoutPhotosField/>, props);

    const checkbox = await queryByRole('checkbox');

    expect(checkbox).not.toBeInTheDocument();
});

it('на странице редактирования поффера не показываем данный чекбокс', async() => {
    const state = {
        config: configStateMock.withPageParams({ form_type: 'edit' }).value(),
    };

    const props = {
        initialValues: {
            [OfferFormFieldNames.PHOTOS]: [ photoMock ],
            [OfferFormFieldNames.WITHOUT_PHOTOS]: true,
        },
        state,
    };

    const { queryByRole } = await renderComponent(<OfferFormWithoutPhotosField/>, props);

    const checkbox = await queryByRole('checkbox');

    expect(checkbox).not.toBeInTheDocument();
});
