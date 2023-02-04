jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(() => Promise.resolve({ auth: false })),
}));
import React from 'react';
import { act } from '@testing-library/react';

import '@testing-library/jest-dom';

import { getResource } from 'auto-core/react/lib/gateApi';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import configMock from 'auto-core/react/dataDomain/config/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { CommunicationType, OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import { OfferBlockContacts } from './OfferBlockContacts';

const originalReplaceState = window.history.replaceState;

beforeAll(() => {
    window.history.replaceState = jest.fn();
});

afterAll(() => {
    window.history.replaceState = jest.fn(originalReplaceState);
});

beforeEach(() => {
    (getResource as jest.MockedFunction<typeof getResource>).mockImplementation((method) => {
        if (method === 'offerCanCreateRedirect') {
            return Promise.resolve(true);
        } else {
            return Promise.resolve({ auth: false });
        }
    });
});

const baseStore = {
    config: configMock
        .withPageParams({
            param1: 'value1',
        })
        .value(),
    user: userMock
        .withAuth(true)
        .withPhones([
            {
                phone: '78762342145',
                phone_formatted: '78762342145',
            },
        ])
        .withAlias('username')
        .value(),
    offerDraft: offerDraftMock.value(),
};

it('при определении, что подменников нет, а так же при удалении всех телефонов сразу выставляется отсутствие подменников + не беспокоить', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferBlockContacts/>,
        {
            formApi,
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.ALL,
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
                [ OfferFormFieldNames.REDIRECT_PHONES ]: true,
                [ OfferFormFieldNames.NO_DISTURB ]: true,
            },
        },
    );

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.PHONES, []);
    });

    expect(getResource).toHaveBeenCalledTimes(1);
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.REDIRECT_PHONES)).toEqual(false);
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.NO_DISTURB)).toEqual(false);
});

it('при добавлении телефонов, проверяется наличие подменников', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferBlockContacts/>,
        {
            formApi,
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.ALL,
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
                [ OfferFormFieldNames.REDIRECT_PHONES ]: false,
            },
        },
    );

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.PHONES, [
            {
                phone: '123123123',
                callFrom: 3,
                callTill: 22,
            },
            {
                phone: '123123124',
                callFrom: 3,
                callTill: 22,
            },
        ]);
    });

    expect(getResource).toHaveBeenCalledTimes(2);
    expect(getResource).toHaveBeenNthCalledWith(1, 'offerCanCreateRedirect', {
        phones: [ '123123123' ],
        geo: undefined,
    });
    expect(getResource).toHaveBeenNthCalledWith(2, 'offerCanCreateRedirect', {
        phones: [ '123123123', '123123124' ],
        geo: undefined,
    });
});

it('при изменении геопозиции, проверяется наличие подменников', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferBlockContacts/>,
        {
            formApi,
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.ALL,
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
                [ OfferFormFieldNames.REDIRECT_PHONES ]: false,
            },
        },
    );

    await act(async() => {
        formApi.current?.setFieldValue(OfferFormFieldNames.LOCATION, {
            geobaseId: '2',
        });
    });

    expect(getResource).toHaveBeenCalledTimes(2);
    expect(getResource).toHaveBeenNthCalledWith(1, 'offerCanCreateRedirect', {
        phones: [ '123123123' ],
        geo: undefined,
    });
    expect(getResource).toHaveBeenNthCalledWith(2, 'offerCanCreateRedirect', {
        phones: [ '123123123' ],
        geo: '2',
    });
});
