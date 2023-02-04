import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
// eslint-disable-next-line max-len
import { mortgageApplicationFormReducer } from 'realty-core/view/react/modules/mortgage/mortgage-application-form/redux/reducer';
// eslint-disable-next-line max-len
import {
    MortgageApplicationConfirmationErrors,
    MortgageApplicationCreationErrors,
} from 'realty-core/types/mortgage/mortgageApplicationFrom';

import { MortgageApplicationFormContainer } from '../container';
import styles from '../styles.module.css';

const badPhone = '+79999999999';
const incorrectPhone = '+78942345623';
const goodCode = '123456';
const wrongCode = '123457';

const Gate = {
    create: (path: string, params: Record<string, string>) => {
        if (path === 'mortgage.createMortgageApplication') {
            if (params.phone === badPhone) {
                return Promise.reject({});
            }

            if (params.phone === incorrectPhone) {
                return Promise.reject({ code: MortgageApplicationCreationErrors.WRONG_PHONE_NUMBER });
            }

            return Promise.resolve({ id: '12-34', codeLength: 6 });
        }

        if (path === 'mortgage.confirmMortgageApplication') {
            if (params.code === goodCode) {
                return Promise.resolve();
            }

            if (params.code === wrongCode) {
                return Promise.reject({ code: MortgageApplicationConfirmationErrors.WRONG_CONFIRMATION_CODE });
            }

            return Promise.reject({});
        }
    },
};

const rootReducer = createRootReducer({ mortgageApplicationForm: mortgageApplicationFormReducer });
const initialState = {
    user: {
        defaultPhone: '+79998887766',
        defaultEmail: 'my-email@yandex.ru',
    },
};

const commonProps = {
    bankId: '1',
    isOpened: true,
};

async function fillForm({
    lastName = 'Грозный',
    firstName = 'Иван',
    middleName = 'Васильевич',
    email = 'my@mail.ru',
    phone = '+73423943233',
}) {
    await [lastName, firstName, middleName, email, phone].reduce((acc, text, index) => {
        return acc.then(() =>
            page.type(`.${styles.applicationScreenLabel}:nth-of-type(${index + 1}) + div input`, text)
        );
    }, Promise.resolve());

    return page.click(`.${styles.applicationScreenCheckbox}`);
}

describe('MortgageApplicationForm', () => {
    it('рисует в дефолтном состоянии', async () => {
        await render(
            <AppProvider rootReducer={rootReducer}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с предзаполненными полями из паспорта', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с ошибками валидации - пустые поля', async () => {
        await render(
            <AppProvider rootReducer={rootReducer}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 800 } }
        );

        await page.click(`.${styles.applicationScreenCheckbox}`);
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с ошибками валидации - по значению', async () => {
        await render(
            <AppProvider rootReducer={rootReducer}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({ email: 'mymail.ru', phone: '+73423kkk3' });
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с подтверждением, если форма отправилась', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({});
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с ошибкой, если запрос свалился', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({ phone: badPhone });
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с ошибкой, если некорректный номер', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({ phone: incorrectPhone });
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с формой, если нажать на телефон на экране подтверждения', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({});
        await page.click(`.${styles.applicationScreenSubmitButton}`);
        await page.click(`.${styles.smsConfirmationText} .Link`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку при вводе неправильного кода на экране подтверждения', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({});
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        await page.type(`.${styles.smsConfirmationInput} input`, wrongCode);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку на экране подтверждения если свалился бек', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({});
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        await page.type(`.${styles.smsConfirmationInput} input`, '6x66666');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с успехом, если код правильный', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} Gate={Gate}>
                <MortgageApplicationFormContainer {...commonProps} />
            </AppProvider>,
            { viewport: { width: 800, height: 700 } }
        );

        await fillForm({});
        await page.click(`.${styles.applicationScreenSubmitButton}`);

        await page.type(`.${styles.smsConfirmationInput} input`, goodCode);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
