import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/lib/test-helpers';
import { IStore } from 'view/common/reducers';

import { SettingsRequisitesFormContainer } from '../container';

import { getStore } from './stubs/store';

interface ITestComponentProps {
    isCreating?: boolean;
    store: DeepPartial<IStore>;
}

const Component: React.FunctionComponent<ITestComponentProps> = ({ store, isCreating }) => (
    <AppProvider initialState={store}>
        <WithScrollContextProvider offset={0}>
            <SettingsRequisitesFormContainer onClose={noop} isEditing={!isCreating} />
        </WithScrollContextProvider>
    </AppProvider>
);

const submitButtonSelector = '#settings_requisites_submit_form .Button';
const emailInputSelector = '#settings_requisites_email .TextInput__control';
const phoneInputSelector = '#settings_requisites_phone .TextInput__control';
const postalCodeInputSelector = '#settings_requisites_postcode .TextInput__control';
const innInputSelector = '#settings_requisites_inn .TextInput__control';
const kppInputSelector = '#settings_requisites_required_kpp .TextInput__control';

describe('SettingsRequisitesForm', () => {
    describe('Отображение', () => {
        it('базовое на 1000', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1000, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('базовое на 1400', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1400, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('базовое на 1000 физик', async () => {
            await render(<Component store={getStore().filledNatural} />, { viewport: { width: 1000, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('базовое на 1400 физик', async () => {
            await render(<Component store={getStore().filledNatural} />, { viewport: { width: 1400, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('создание на 1000 физик', async () => {
            await render(<Component isCreating store={getStore().defaultNatural} />, {
                viewport: { width: 1000, height: 700 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('создание на 1400 физик', async () => {
            await render(<Component isCreating store={getStore().defaultNatural} />, {
                viewport: { width: 1400, height: 700 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('сохраненный на 1000', async () => {
            await render(<Component store={getStore().saved} />, { viewport: { width: 1000, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('сохраненный на 1400', async () => {
            await render(<Component store={getStore().saved} />, { viewport: { width: 1400, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с предзаполненными полями из vos 1000', async () => {
            await render(<Component isCreating store={getStore().withFilledVosUserData} />, {
                viewport: { width: 1000, height: 700 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с предзаполненными полями из vos 1400', async () => {
            await render(<Component isCreating store={getStore().withFilledVosUserData} />, {
                viewport: { width: 1400, height: 700 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('ошибки', () => {
        it('billingRequisites не загруженны 1000', async () => {
            await render(<Component store={getStore().notLoaded} />, { viewport: { width: 1000, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('billingRequisites не загруженны 1400', async () => {
            await render(<Component store={getStore().notLoaded} />, { viewport: { width: 1400, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('не заполненный 1000', async () => {
            await render(<Component store={getStore().notFilled} />, { viewport: { width: 1000, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('не заполненный 1400', async () => {
            await render(<Component store={getStore().notFilled} />, { viewport: { width: 1400, height: 700 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неверный ввод 1000', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1000, height: 700 } });

            await page.type(emailInputSelector, 'Почта');
            await page.type(phoneInputSelector, '7999');
            await page.type(postalCodeInputSelector, 'Циферки');
            await page.type(kppInputSelector, '1337');
            await page.type(innInputSelector, '12345');
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            await page.$eval(innInputSelector, (e) => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неверный ввод 1400', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1400, height: 700 } });

            await page.type(emailInputSelector, 'Почта');
            await page.type(phoneInputSelector, '7999');
            await page.type(postalCodeInputSelector, 'Циферки');
            await page.type(kppInputSelector, '1337');
            await page.type(innInputSelector, '12345');
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            await page.$eval(innInputSelector, (e) => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('логика', () => {
        it('показ ошибок на blur', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1000, height: 700 } });

            await page.focus(emailInputSelector);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            await page.$eval(emailInputSelector, (e) => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(emailInputSelector, '13332@list');
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            await page.$eval(emailInputSelector, (e) => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('KПП не обязателен для индивидуального с ИИН в 12 чисел', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1000, height: 700 } });

            await page.type(innInputSelector, '910403356900');

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('KПП обязателен для индивидуального с ИИН в 10 чисел', async () => {
            await render(<Component store={getStore().default} />, { viewport: { width: 1000, height: 700 } });

            await page.type(innInputSelector, '7704407589');

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
