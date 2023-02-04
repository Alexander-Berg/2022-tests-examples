import React, { createRef } from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import fetchMock from 'jest-fetch-mock';
import '@testing-library/jest-dom';
import { simulateControlClick } from 'jest/unit/eventSimulators';

import sleep from 'auto-core/lib/sleep';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { successfullyAuthorizeUser } from 'auto-core/react/components/common/LazyPhoneAuthAbstract/testUtils/successfullyAuthorizeUser';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import type { AppState } from 'www-poffer/react/store/AppState';

import OfferFormPhonesField from './OfferFormPhonesField';

afterEach(() => {
    fetchMock.resetMocks();
});

const baseStore = {
    user: userMock
        .withAuth(true)
        .withPhones([
            {
                phone: '8676523948764',
                phone_formatted: '8676523948764',
            },
        ])
        .value(),
    offerDraft: offerDraftMock.value(),
};

//
//
// при клике в крестик третьего телефона, показываем кнопку "добавить"

it('нет телефонов - показываем ленивую авторизацию', async() => {
    await renderComponent(
        (
            <OfferFormPhonesField
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                cooldownInRow
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [],
            },
        },
    );

    const phoneEditInput = document.querySelector('[name=phone]');
    const phoneEditInputCallFrom = document.querySelector('[name=callFrom]');
    const phoneEditInputCallTill = document.querySelector('[name=callTill]');
    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneEditInput).toBeNull();
    expect(phoneEditInputCallFrom).toBeNull();
    expect(phoneEditInputCallTill).toBeNull();
    expect(phoneConfirmButton).not.toBeNull();
    expect(phoneAddButton).toBeNull();
});

it('есть телефон - показываем кнопку "добавить" и phoneInput с селектами времени', async() => {
    await renderComponent(
        (
            <OfferFormPhonesField
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                cooldownInRow
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const phoneEditInput = document.querySelector('[name=phone]');
    const phoneEditInputCallFrom = document.querySelector('[name=callFrom]');
    const phoneEditInputCallTill = document.querySelector('[name=callTill]');
    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneEditInput).not.toBeNull();
    expect(phoneEditInputCallFrom).not.toBeNull();
    expect(phoneEditInputCallTill).not.toBeNull();
    expect(phoneConfirmButton).toBeNull();
    expect(phoneAddButton).not.toBeNull();
});

it('есть 3 телефона - не показываем кнопку добавить', async() => {
    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const phoneEditInput = document.querySelectorAll('[name=phone]');
    const phoneEditInputCallFrom = document.querySelectorAll('[name=callFrom]');
    const phoneEditInputCallTill = document.querySelectorAll('[name=callTill]');
    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneEditInput).toHaveLength(3);
    expect(phoneEditInputCallFrom).toHaveLength(3);
    expect(phoneEditInputCallTill).toHaveLength(3);
    expect(phoneConfirmButton).toBeNull();
    expect(phoneAddButton).toBeNull();
});

it('при клике в "добавить", скрываем кнопку и показываем ленивую авторизацию', async() => {
    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.getByText('Добавить ещё номер');

    expect(phoneConfirmButton).toBeNull();
    expect(phoneAddButton).not.toBeNull();

    userEvent.click(phoneAddButton);

    expect(screen.queryByText('Подтвердить номер')).not.toBeNull();
    expect(screen.queryByText('Добавить ещё номер')).toBeNull();
});

it('при добавлении телефона скрываем ленивую авторизацию, показываем phoneInput, добавляем телефон в форму', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            formApi,
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [],
            },
            offerFormContext: offerFormPageContextMock,
        },
    );

    const phone = '79266543221';

    await successfullyAuthorizeUser(phone);

    const phoneEditInput = document.querySelectorAll('[name=phone]');
    const phoneEditInputCallFrom = document.querySelectorAll('[name=callFrom]');
    const phoneEditInputCallTill = document.querySelectorAll('[name=callTill]');
    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneEditInput).toHaveLength(1);
    expect(phoneEditInputCallFrom).toHaveLength(1);
    expect(phoneEditInputCallTill).toHaveLength(1);
    expect(phoneConfirmButton).toBeNull();
    expect(phoneAddButton).not.toBeNull();

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([
        {
            phone,
            callFrom: 9,
            callTill: 21,
        },
    ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({
        field: OfferFormFieldNames.PHONES,
        event: 'success',
        level_6: 'confirmed',
    });
});

it('при изменении селектов, обновляем телефоны в форме', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            formApi,
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '79871234567',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '79877654321',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
            offerFormContext: offerFormPageContextMock,
        },
    );

    const firstFromSelect = screen.queryAllByText('с 9:00');
    const firstTillSelect = screen.queryAllByText('до 21:00');

    await act(async() => {
        await simulateControlClick(firstFromSelect[0]);

        await sleep(10);

        const menuItem = await screen.getByText('с 8:00');

        userEvent.click(menuItem);
    });

    await act(async() => {
        await simulateControlClick(firstTillSelect[0]);

        await sleep(10);

        const menuItem = await screen.getByText('до 22:00');

        userEvent.click(menuItem);
    });

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([
        {
            phone: '79871234567',
            callFrom: 8,
            callTill: 22,
        },
        {
            phone: '79877654321',
            callFrom: 9,
            callTill: 21,
        },
    ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { field: OfferFormFieldNames.PHONES + '_time_from', event: 'click' });
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { field: OfferFormFieldNames.PHONES + '_time_to', event: 'click' });
});

it('при клике в крестик телефона, удаляем телефон, скрываем phoneInput', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            formApi,
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '79871234567',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '79877654321',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const closer = document.querySelector('.TextInput__clear');

    if (!closer) {
        throw 'No closer element found.';
    }

    await act(async() => {
        await userEvent.click(closer);
    });

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([
        {
            phone: '79877654321',
            callFrom: 9,
            callTill: 21,
        },
    ]);
});

it('при клике в крестик единственного телефона, скрываем phoneInput и показываем ленивую авторизацию', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            formApi,
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '79871234567',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const closer = document.querySelector('.TextInput__clear');

    if (!closer) {
        throw 'No closer element found.';
    }

    await act(async() => {
        await userEvent.click(closer);
    });

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PHONES)).toEqual([]);

    const phoneEditInput = document.querySelector('[name=phone]');
    const phoneEditInputCallFrom = document.querySelector('[name=callFrom]');
    const phoneEditInputCallTill = document.querySelector('[name=callTill]');
    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneEditInput).toBeNull();
    expect(phoneEditInputCallFrom).toBeNull();
    expect(phoneEditInputCallTill).toBeNull();
    expect(phoneConfirmButton).not.toBeNull();
    expect(phoneAddButton).toBeNull();
});

it('при клике в крестик третьего телефона, показываем кнопку "добавить"', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(
        (
            <OfferFormPhonesField
                cooldownInRow
                withCallTimeTable
                inputClassName=""
                codeInputClassName=""
                cooldownButtonClassName=""
                rowClassName=""
                isMobile={ false }
            />
        ),
        {
            formApi,
            state: {
                ...baseStore,
            } as AppState,
            initialValues: {
                [ OfferFormFieldNames.PHONES ]: [
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                    {
                        phone: '123123123',
                        callFrom: 9,
                        callTill: 21,
                    },
                ],
            },
        },
    );

    const closer = document.querySelectorAll('.TextInput__clear')[2];

    if (!closer) {
        throw 'No closer element found.';
    }

    await act(async() => {
        await userEvent.click(closer);
    });

    const phoneConfirmButton = screen.queryByText('Подтвердить номер');
    const phoneAddButton = screen.queryByText('Добавить ещё номер');

    expect(phoneConfirmButton).toBeNull();
    expect(phoneAddButton).not.toBeNull();
});
