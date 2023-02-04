import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import contextMock from 'autoru-frontend/mocks/contextMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { ActionQueueContext } from 'auto-core/react/components/common/ActionQueue/ActionQueue';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import OfferFormOnlineViewField
    from './OfferFormOnlineViewField';

const state = {
    config: configStateMock.withPageParams({}).value(),
};

it('при выборе значения в чекбоксе отсылается метрика', async() => {
    expect.assertions(1);

    const props = {
        state,
        initialValues: {
            [OfferFormFieldNames.ONLINE_VIEW_AVAILABLE]: true,
        },
    };

    await renderComponent(<OfferFormOnlineViewField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});

it('на странице создания поффера посылает стату из очереди', async() => {
    expect.assertions(2);

    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const statActionQueueApi = React.createRef<ActionQueueContext>();

    const props = {
        state,
        formApi,
        statActionQueueApi,
    };

    await renderComponent(<OfferFormOnlineViewField/>, props);

    await statActionQueueApi.current?.runCallbacks();
    await flushPromises();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(statActionQueueApi.current?.callbacks?.length).toEqual(1);
});

it('на странице редактирования поффера посылает стату из очереди, если значения поменялись', async() => {
    expect.assertions(2);

    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const statActionQueueApi = React.createRef<ActionQueueContext>();

    const state = {
        config: configStateMock.withPageParams({ form_type: 'edit' }).value(),
    };

    const initialValues = {
        [OfferFormFieldNames.ONLINE_VIEW_AVAILABLE]: false,
    };

    const props = {
        state,
        formApi,
        statActionQueueApi,
        initialValues,
    };

    await renderComponent(<OfferFormOnlineViewField/>, props);

    const checkbox = screen.getByRole('checkbox');

    userEvent.click(checkbox);

    await statActionQueueApi.current?.runCallbacks();

    await flushPromises();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(statActionQueueApi.current?.callbacks?.length).toEqual(1);
});

it('на странице редактирования поффера не посылает стату из очереди, если значения не поменялись', async() => {
    expect.assertions(2);

    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const statActionQueueApi = React.createRef<ActionQueueContext>();

    const state = {
        config: configStateMock.withPageParams({ form_type: 'edit' }).value(),
    };

    const initialValues = {
        [OfferFormFieldNames.ONLINE_VIEW_AVAILABLE]: false,
    };

    const props = {
        state,
        formApi,
        statActionQueueApi,
        initialValues,
    };

    await renderComponent(<OfferFormOnlineViewField/>, props);

    await statActionQueueApi.current?.runCallbacks();

    await flushPromises();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(0);
    expect(statActionQueueApi.current?.callbacks?.length).toEqual(1);
});
