import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import userPersonalDataPhoneFormStyles from 'view/components/Modal/UserPersonalDataPhoneModal/styles.module.css';
import FormRepeatConfirmationCodeStyles from 'view/components/Form/FormRepeatConfirmationCode/styles.module.css';

import 'view/styles/common.css';

import { UserPersonalDataPhoneModalContainer } from '../container';

import { store, confirmationStore } from './stub/store';

const selectors = {
    submitBtn: `.${userPersonalDataPhoneFormStyles.button}`,
    repeatConfirmBtn: `.${FormRepeatConfirmationCodeStyles.link}`,
    phoneInput: '#PHONE',
    confirmInput: '#CONFIRMATION_CODE',
};

const renderOptions = [
    { viewport: { width: 415, height: 600 } },
    { viewport: { width: 375, height: 600 } },
    { viewport: { width: 1000, height: 1000 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        rootReducer={userReducer}
        Gate={props.Gate}
        initialState={props.store}
        fakeTimers={{
            now: new Date('2020-06-02T03:00:00.111Z').getTime(),
        }}
    >
        <UserPersonalDataPhoneModalContainer isOpen={true} closeModal={() => undefined} />
    </AppProvider>
);

describe('UserPersonalDataPhoneModal', () => {
    renderOptions.forEach((renderOption) => {
        it(`Базовое состояние ${renderOption.viewport.width} px`, async () => {
            await render(<Component store={store} />, renderOption);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('Неверный формат номера телефона', async () => {
        await render(<Component store={store} />, renderOptions[1]);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(selectors.phoneInput, '98765');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.submitBtn);

        await customPage.tick(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Убрать ошибку при редактировании телефона', async () => {
        await render(<Component store={store} />, renderOptions[1]);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(selectors.phoneInput, '98765');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.submitBtn);

        await customPage.tick(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(selectors.phoneInput, '98');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('СМС была отправлена', () => {
        it(`Не указали код`, async () => {
            await render(<Component store={confirmationStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Фронтовая валидация кода`, async () => {
            await render(<Component store={confirmationStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.confirmInput, '123');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Ручка бекенда прислала, что код некорректный`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.confirm_phone_confirmation_code': {
                            return Promise.resolve({ confirmationFailed: { reason: 'WRONG_CODE' } });
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.confirmInput, '12345');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
        it(`Ручка бекенда подтверждения упала`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.confirm_phone_confirmation_code': {
                            return Promise.reject();
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.confirmInput, '12345');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Ручка бекенда ответила успешно`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.confirm_phone_confirmation_code': {
                            return Promise.resolve({});
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.confirmInput, '12345');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Ручка бекенда в процессе ответа`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.confirm_phone_confirmation_code': {
                            return new Promise(noop);
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.confirmInput, '12345');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Повторная отправка смс', () => {
        it(`Повторная отправка смс в процессе`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_phone_confirmation_sms': {
                            return new Promise(noop);
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.repeatConfirmBtn);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Ручка бекенда ответила успешно и после изменили телефон`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_phone_confirmation_sms': {
                            return Promise.resolve({
                                sendSuccess: {
                                    codeLength: 5,
                                    requestId: '123',
                                },
                            });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.phoneInput, '9876543210');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.phoneInput);
            await page.keyboard.press('Backspace');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Повторная отправка смс успешна`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_phone_confirmation_sms': {
                            return Promise.resolve({ sendSuccess: { codeLength: 5, requestId: '123' } });
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.repeatConfirmBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Повторная отправка смс ошибка`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_phone_confirmation_sms': {
                            return Promise.reject();
                        }
                    }
                },
            };

            await render(<Component store={confirmationStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.repeatConfirmBtn);

            await customPage.tick(1000);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
