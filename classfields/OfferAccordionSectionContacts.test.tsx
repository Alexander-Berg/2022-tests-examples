jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(() => Promise.resolve({ auth: false })),
}));
import React from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import mockStore from 'autoru-frontend/mocks/mockStore';

import { getResource } from 'auto-core/react/lib/gateApi';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import configMock from 'auto-core/react/dataDomain/config/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { OfferAccordionSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';
import { CommunicationType, OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import OfferAccordionSectionContacts from './OfferAccordionSectionContacts';

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

it('устанавливает значения, есть что установить из драфта', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.USERNAME ]: 'username1',
                [ OfferFormFieldNames.USEREMAIL ]: 'email@email.com1',
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '7876234214521',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
            },
        },
    );

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /как к вам обращаться\?/i }).value).toEqual('username1');
    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /электронная почта \(e-mail\)/i }).value).toEqual('email@email.com1');

    const phoneInputs = screen.getAllByRole<HTMLInputElement>('textbox', { name: /Номер телефона/i });
    const selects = screen.getAllByRole('listbox');

    expect(phoneInputs).toHaveLength(1);
    expect(selects).toHaveLength(2);

    expect(phoneInputs[0].value).toEqual('+7 876 234-21-45');
    expect(selects[0].textContent).toEqual('с 3:00');
    expect(selects[1].textContent).toEqual('до 22:00');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USERNAME)).toEqual('username1');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USEREMAIL)).toEqual('email@email.com1');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([
        {
            phone: '7876234214521',
            callFrom: 3,
            callTill: 22,
        },
    ]);
});

it('устанавливает значения по-умолчанию, если юзер авторизован, есть что установить и в драфте пусто', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: baseStore,
        },
    );

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /как к вам обращаться\?/i }).value).toEqual('username');
    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /электронная почта \(e-mail\)/i }).value).toEqual('john.doe@yandex.ru');

    const phoneInputs = screen.getAllByRole<HTMLInputElement>('textbox', { name: /Номер телефона/i });
    const selects = screen.getAllByRole('listbox');

    expect(phoneInputs).toHaveLength(1);
    expect(selects).toHaveLength(2);

    expect(phoneInputs[0].value).toEqual('+7 876 234-21-45');
    expect(selects[0].textContent).toEqual('с 9:00');
    expect(selects[1].textContent).toEqual('до 21:00');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USERNAME)).toEqual('username');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USEREMAIL)).toEqual('john.doe@yandex.ru');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([
        {
            phone: '78762342145',
            callFrom: 9,
            callTill: 21,
        },
    ]);
});

it('устанавливает имя по-умолчанию, если юзер авторизован, нет имени в пользователе, и нет его ника, и в драфте пусто', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .withAlias('')
                    .value(),
            },
        },
    );

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /как к вам обращаться\?/i }).value).toEqual('');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USERNAME)).toEqual('');
});

it('НЕ устанавливает имя по-умолчанию, если юзер авторизован, в имени лежит email и в драфте пусто', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .withAlias('email@email.com')
                    .value(),
            },
        },
    );

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /как к вам обращаться\?/i }).value).toEqual('');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USERNAME)).toEqual('');
});

it('НЕ устанавливает email по-умолчанию, если юзер авторизован, в сторе пусто и в драфте пусто', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .withEmails([])
                    .value(),
            },
        },
    );

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /электронная почта \(e-mail\)/i }).value).toEqual('');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USEREMAIL)).toEqual(undefined);
});

it('НЕ устанавливает телефоны по-умолчанию, если юзер авторизован, в сторе пусто и в драфте пусто', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .value(),
            },
        },
    );

    const selects = screen.queryAllByRole('listbox');

    expect(selects).toHaveLength(0);

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual(undefined);
});

it('обновляет поля при авторизации (кроме телефона, он будет тем, которым авторизовались, но в тесте тупо не обновится)', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const state = {
        ...baseStore,
        user: userMock
            .withAuth(false)
            .value(),
    };
    const storeMock = mockStore(state);

    const {
        rerender,
    } = await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            storeMock,
            state,
        },
    );

    (storeMock.getState as jest.MockedFunction<typeof storeMock.getState>).mockImplementation(() => ({
        ...baseStore,
        user: userMock
            .withAuth(true)
            .withAlias('username')
            .value(),
    }));

    await rerender(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            storeMock,
            state,
        },
    );

    expect(window.history.replaceState).not.toHaveBeenCalled();

    const selects = screen.queryAllByRole('listbox');

    expect(selects).toHaveLength(0);

    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /как к вам обращаться\?/i }).value).toEqual('username');
    expect(screen.getByRole<HTMLInputElement>('textbox', { name: /электронная почта \(e-mail\)/i }).value).toEqual('john.doe@yandex.ru');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USERNAME)).toEqual('username');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.USEREMAIL)).toEqual('john.doe@yandex.ru');
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual(undefined);
});

it('при авторизации дилером редиректит в старую форму', async() => {
    const state = {
        ...baseStore,
        user: userMock
            .withAuth(false)
            .value(),
    };
    const storeMock = mockStore(state);

    const {
        rerender,
    } = await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            storeMock,
            state,
        },
    );

    (storeMock.getState as jest.MockedFunction<typeof storeMock.getState>).mockImplementation(() => ({
        ...baseStore,
        user: userMock
            .withAuth(true)
            .withClientId('dealer id')
            .value(),
    }));

    await rerender(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            storeMock,
            state,
        },
    );

    expect(window.history.replaceState).toHaveBeenCalledTimes(1);
    expect(window.history.replaceState).toHaveBeenCalledWith(null, '', 'link/form/?param1=value1');
});

it('если есть телефоны, показывается выбор типа коммуникации', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            state: baseStore,
        },
    );

    expect(screen.queryByText(/способ связи/i)).not.toBeNull();
});

it('если НЕТ телефонов, НЕ показывается выбор типа коммуникации', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .value(),
            },
        },
    );

    expect(screen.queryByText(/способ связи/i)).toBeNull();
});

it('показываются чекбоксы, если тип коммуникации "все", есть телефоны и есть подменники', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
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
            },
        },
    );

    expect(screen.queryByText(/дополнительно/i)).not.toBeNull();
});

it('показываются чекбоксы, если тип коммуникации не "телефон", есть телефоны и есть подменники', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.PHONE_ONLY,
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
            },
        },
    );

    expect(screen.queryByText(/дополнительно/i)).not.toBeNull();
});

it('НЕ показываются чекбоксы, если тип коммуникации "только чат", есть телефоны и есть подменники', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.CHAT_ONLY,
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 3,
                        callTill: 22,
                    },
                ],
            },
        },
    );

    expect(screen.queryByText(/дополнительно/i)).toBeNull();
});

it('НЕ показываются чекбоксы, если тип коммуникации не "только чат", нет телефонов и есть подменники', async() => {
    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            state: {
                ...baseStore,
                user: userMock
                    .withAuth(true)
                    .value(),
            },
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.ALL,
                [ OfferFormFieldNames.PHONES ]: [],
            },
        },
    );

    expect(screen.queryByText(/дополнительно/i)).toBeNull();
});

it('НЕ показываются чекбоксы, если тип коммуникации не "только чат", есть телефоны и нет подменников', async() => {
    (getResource as jest.MockedFunction<typeof getResource>).mockImplementation((method) => {
        if (method === 'offerCanCreateRedirect') {
            return Promise.resolve(false);
        } else {
            return Promise.resolve({ auth: false });
        }
    });

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
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
            },
        },
    );

    expect(screen.queryByText(/дополнительно/i)).toBeNull();
});

it('если редиректы выключены, то при показе чекбоксов редиректы включаются', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
        {
            formApi,
            state: baseStore,
            initialValues: {
                [ OfferFormFieldNames.COMMUNICATION_TYPE ]: CommunicationType.CHAT_ONLY,
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

    const firstTag = screen.getByRole('button', { name: /любой способ/i });

    userEvent.click(firstTag);

    expect(screen.getByRole<HTMLInputElement>('checkbox', { name: /Бесплатно защитить номер от спама/i }).checked).toEqual(true);
});

it('при определении, что подменников нет, скидываются чекбоксы, так же при удалении всех телефонов сразу выставляется отсутствие подменников', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
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
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
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
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
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

it('при скидывании галки REDIRECT_PHONES, выключается галка NO_DISTURB', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        <OfferAccordionSectionContacts
            id={ OfferAccordionSectionId.CONTACTS }
            initialIsCollapsed={ false }
            isVisible
        />,
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

    const spam = screen.getByRole<HTMLInputElement>('checkbox', { name: /Бесплатно защитить номер от спама/i });
    const noDistrub = screen.getByRole<HTMLInputElement>('checkbox', { name: /Отказаться от звонков проверенных дилеров/i });

    userEvent.click(spam);

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.NO_DISTURB)).toEqual(false);
    expect(noDistrub.disabled).toEqual(true);
});
