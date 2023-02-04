import React from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { ActionQueueContext } from 'auto-core/react/components/common/ActionQueue/types';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { CommunicationType, OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import type { AppState } from 'www-poffer/react/store/AppState';

import OfferFormCommunicationTypeField from './OfferFormCommunicationTypeField';

const baseStore = {
    config: {
        data: {
            pageParams: {
                lol: 'kol',
            },
        },
    },
};

it('на изменении вызывает изменение в форме, props.onChange и отсылает стату', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const onChange = jest.fn();

    await renderComponent(
        (
            <OfferFormCommunicationTypeField
                onChange={ onChange }
            />
        ),
        {
            state: (baseStore as unknown as AppState),
            offerFormContext: {
                ...offerFormPageContextMock,
            },
            coreContext: {
                ...contextMock,
            },
            formApi,
        },
    );

    const tags = screen.getAllByRole('button');

    userEvent.click(tags[0]);

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.COMMUNICATION_TYPE)).toEqual(CommunicationType.ALL);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith(CommunicationType.ALL);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith(
        {
            field: OfferFormFieldNames.COMMUNICATION_TYPE,
            event: 'click',
            level_6: CommunicationType.ALL,
        },
    );
});

it('на "сабмите" отправляет стату для страницы публикации', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const statActionQueueApi = React.createRef<ActionQueueContext>();

    await renderComponent(
        (
            <OfferFormCommunicationTypeField
            />
        ),
        {
            state: (baseStore as unknown as AppState),
            offerFormContext: {
                ...offerFormPageContextMock,
            },
            coreContext: {
                ...contextMock,
            },
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.ALL,
            },
            statActionQueueApi,
            formApi,
        },
    );

    expect(statActionQueueApi.current?.callbacks.length).toEqual(1);

    await statActionQueueApi.current?.runCallbacks();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(1, [ 'Dont_call_me_settings', 'public', 'turn_off' ]);

    act(() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.COMMUNICATION_TYPE, CommunicationType.CHAT_ONLY);
    });

    await statActionQueueApi.current?.runCallbacks();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'Dont_call_me_settings', 'public', 'turn_on' ]);
});

it('на "сабмите" отправляет стату для страницы редактирования', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const statActionQueueApi = React.createRef<ActionQueueContext>();

    await renderComponent(
        (
            <OfferFormCommunicationTypeField
            />
        ),
        {
            state: ({
                ...baseStore,
                config: {
                    data: {
                        ...baseStore.config.data,
                        pageParams: {
                            form_type: 'edit',
                        },
                    },
                },
            } as unknown as AppState),
            offerFormContext: {
                ...offerFormPageContextMock,
            },
            coreContext: {
                ...contextMock,
            },
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.PHONE_ONLY,
            },
            statActionQueueApi,
            formApi,
        },
    );

    expect(statActionQueueApi.current?.callbacks.length).toEqual(1);

    await statActionQueueApi.current?.runCallbacks();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(0);

    act(() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.COMMUNICATION_TYPE, CommunicationType.ALL);
    });

    await statActionQueueApi.current?.runCallbacks();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(1, [ 'Dont_call_me_settings', 'edit', 'turn_off' ]);

    act(() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.COMMUNICATION_TYPE, CommunicationType.CHAT_ONLY);
    });

    await statActionQueueApi.current?.runCallbacks();

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'Dont_call_me_settings', 'edit', 'turn_on' ]);
});
