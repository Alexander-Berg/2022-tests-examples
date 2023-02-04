import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import fetchMock from 'jest-fetch-mock';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import sleep from 'auto-core/lib/sleep';

export const successfullyAuthorizeUser = async(phone: string) => {
    const phoneInput = document.querySelector('.LazyPhoneAuth__phoneInput input');
    const addPhoneButton = document.querySelector('.LazyPhoneAuth__button');

    if (!phoneInput || !addPhoneButton) {
        throw `Не удалось найти контролы! phoneInput: ${ Boolean(phoneInput) }; addPhoneButton: ${ Boolean(addPhoneButton) }`;
    }

    fetchMock.once(JSON.stringify({
        need_confirm: true,
        code_length: 4,
        status: 'SUCCESS',
    }));

    await act(async() => {
        await userEvent.clear(phoneInput);
        await userEvent.type(phoneInput, phone.startsWith('7') ? phone.substr(1) : phone);
        await userEvent.click(addPhoneButton);
    });

    /*
        POST -/ajax/poffer/addPhone?phone=79262620158
     */

    await sleep(0);
    await flushPromises();

    const codeInput = document.querySelector('.LazyPhoneAuth__codeInput input');
    const codeSendButton = document.querySelector('.LazyPhoneAuth__codeResendButton');

    if (!codeInput || !codeSendButton) {
        throw `Не удалось найти контролы! codeInput: ${ Boolean(codeInput) }; codeSendButton: ${ Boolean(codeSendButton) }`;
    }

    const code = '3432';

    fetchMock.once(JSON.stringify([
        {
            user: {
                auth: true,
            },
        },
    ]));

    userEvent.type(codeInput, code);

    await act(async() => {
        userEvent.click(codeSendButton);
    });

    /*
        Request URL: https://auth.auth.aandrosov.dev.vertis.yandex.net/-/ajax/auth/
        Request Method: POST
    */

    await sleep(0);
    await flushPromises();
};
