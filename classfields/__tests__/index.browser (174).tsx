import React from 'react';
import noop from 'lodash/noop';
import { DeepPartial } from 'utility-types';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyFunction, AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/lib/test-helpers';
import { IStore, rootReducer } from 'view/common/reducers';

import { SettingsRequisitesListContainer } from '../container';

import listStyles from '../styles.module.css';
import formStyles from '../../SettingsRequisitesForm/styles.module.css';

import { getStore } from './stubs/store';

const viewports = [
    { width: 1000, height: 600 },
    { width: 1200, height: 600 },
] as const;

const selectors = {
    addButtonSelector: `.${listStyles.add}`,
    cardSelectorFactory: (n: number) => `.${listStyles.container} .${listStyles.card}:nth-child(${n})`,
    cardCancelButtonSelectorFactory: (n: number) =>
        `.${listStyles.container} .${listStyles.card}:nth-child(${n}) .${formStyles.cancelButton}`,
    formFiledsSelectors: {
        nameInputSelector: '#settings_requisites_name .TextInput__control',
        emailInputSelector: '#settings_requisites_email .TextInput__control',
        phoneInputSelector: '#settings_requisites_phone .TextInput__control',
        postalCodeInputSelector: '#settings_requisites_postcode .TextInput__control',
        legaladdressInputSelector: '#settings_requisites_legaladdress .TextInput__control',
        postaddressInputSelector: '#settings_requisites_postaddress .TextInput__control',
        innInputSelector: '#settings_requisites_inn .TextInput__control',
        kppInputSelector: '#settings_requisites_required_kpp .TextInput__control',
        submitButtonSelector: '#settings_requisites_submit_form .Button',
    },
} as const;
interface ITestComponentProps {
    state: DeepPartial<IStore>;
    Gate?: AnyObject;
}

const Component: React.FunctionComponent<ITestComponentProps> = ({ state, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} Gate={Gate} initialState={state}>
            <SettingsRequisitesListContainer />
        </AppProvider>
    );
};

const render = async (component: React.ReactElement, fn?: AnyFunction) => {
    for (const viewport of viewports) {
        await _render(component, { viewport });

        await fn?.();

        expect(
            await takeScreenshot({
                fullPage: true,
                keepCursor: true,
            })
        ).toMatchImageSnapshot();
    }
};

describe('SettingsRequisitesList', () => {
    it('рендерится корректно', async () => {
        await render(<Component state={getStore()} />);
    });

    it('c ховером', async () => {
        await render(<Component state={getStore()} />, async () => {
            await page.hover(selectors.cardSelectorFactory(1));
        });
    });

    it('открытие, закрытие формы редактирования', async () => {
        await render(<Component state={getStore()} />, async () => {
            await page.click(selectors.cardSelectorFactory(1));

            expect(
                await takeScreenshot({
                    fullPage: true,
                    keepCursor: true,
                })
            ).toMatchImageSnapshot();

            await page.click(selectors.cardCancelButtonSelectorFactory(1));
        });
    });

    it('успешное добавление новых реквизитов', async () => {
        const Gate = {
            create: () => Promise.resolve({ clientId: null, personId: 14344023 }),
        };

        await render(<Component Gate={Gate} state={getStore()} />, async () => {
            await page.click(selectors.addButtonSelector);

            expect(
                await takeScreenshot({
                    fullPage: true,
                    keepCursor: true,
                })
            ).toMatchImageSnapshot();

            const { formFiledsSelectors } = selectors;

            await page.type(formFiledsSelectors.nameInputSelector, 'name');
            await page.type(formFiledsSelectors.legaladdressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.postaddressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.emailInputSelector, 'ztuzmin666@yandex.ru');
            await page.type(formFiledsSelectors.phoneInputSelector, '+79042166212');
            await page.type(formFiledsSelectors.postalCodeInputSelector, '33333333');
            await page.type(formFiledsSelectors.kppInputSelector, '540601001');
            await page.type(formFiledsSelectors.innInputSelector, '5406576757');

            expect(
                await takeScreenshot({
                    fullPage: true,
                    keepCursor: true,
                })
            ).toMatchImageSnapshot();

            await page.click(formFiledsSelectors.submitButtonSelector);
        });
    });

    it('добавление новых реквизитов с ошибкой', async () => {
        const Gate = {
            create: () => Promise.reject(),
        };

        await render(<Component Gate={Gate} state={getStore()} />, async () => {
            await page.click(selectors.addButtonSelector);

            const { formFiledsSelectors } = selectors;

            await page.type(formFiledsSelectors.nameInputSelector, 'name');
            await page.type(formFiledsSelectors.legaladdressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.postaddressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.emailInputSelector, 'ztuzmin666@yandex.ru');
            await page.type(formFiledsSelectors.phoneInputSelector, '+79042166212');
            await page.type(formFiledsSelectors.postalCodeInputSelector, '33333333');
            await page.type(formFiledsSelectors.kppInputSelector, '540601001');
            await page.type(formFiledsSelectors.innInputSelector, '5406576757');

            await page.click(formFiledsSelectors.submitButtonSelector);
        });
    });

    it('добавление новых реквизитов ожидание', async () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        await render(<Component Gate={Gate} state={getStore()} />, async () => {
            await page.click(selectors.addButtonSelector);

            const { formFiledsSelectors } = selectors;

            await page.type(formFiledsSelectors.nameInputSelector, 'name');
            await page.type(formFiledsSelectors.legaladdressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.postaddressInputSelector, 'Пушкина Колотушкина');
            await page.type(formFiledsSelectors.emailInputSelector, 'ztuzmin666@yandex.ru');
            await page.type(formFiledsSelectors.phoneInputSelector, '+79042166212');
            await page.type(formFiledsSelectors.postalCodeInputSelector, '33333333');
            await page.type(formFiledsSelectors.kppInputSelector, '540601001');
            await page.type(formFiledsSelectors.innInputSelector, '5406576757');

            await page.click(formFiledsSelectors.submitButtonSelector);
        });
    });
});
