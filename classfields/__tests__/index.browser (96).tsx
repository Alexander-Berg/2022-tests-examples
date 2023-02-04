import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { UserPaymentMethodsBindCard } from '../index';
import styles from '../styles.module.css';

import { store } from './stub';

const renderOptions = [{ viewport: { width: 580, height: 300 } }];

const Component: React.FunctionComponent<
    {
        store: DeepPartial<IUniversalStore>;
        Gate?: AnyObject;
    } & React.ComponentProps<typeof UserPaymentMethodsBindCard>
> = ({ store, Gate, ...otherProps }) => (
    <AppProvider rootReducer={userReducer} Gate={Gate} initialState={store}>
        <UserPaymentMethodsBindCard {...otherProps} />
    </AppProvider>
);

const selectors = {
    button: `.${styles.button}`,
};

describe('UserPaymentMethodsBindCard', () => {
    describe(`C одним описанием`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={store}
                        initBindCard={() => Promise.resolve({ paymentUrl: '' })}
                        description={'Пока вы не привяжете карту, у вас не получится получать арендную плату'}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`C 2 описаниями`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={store}
                        initBindCard={() => Promise.resolve({ paymentUrl: '' })}
                        description={'Пока вы не привяжете карту, у вас не получится получать арендную плату'}
                        description2={
                            'Не добавляйте кредитные карты, потому что с них нельзя снять деньги без комиссии'
                        }
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`В процессе инициализации`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={store}
                        initBindCard={() => new Promise(noop)}
                        description={'Пока вы не привяжете карту, у вас не получится получать арендную плату'}
                    />,
                    renderOption
                );

                await page.click(selectors.button);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Успешно проинициализировалась`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={store}
                        initBindCard={() => Promise.resolve({ paymentUrl: '' })}
                        description={'Пока вы не привяжете карту, у вас не получится получать арендную плату'}
                    />,
                    renderOption
                );

                await page.click(selectors.button);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Ошибка при инициализации`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={store}
                        initBindCard={() => Promise.reject()}
                        description={'Пока вы не привяжете карту, у вас не получится получать арендную плату'}
                    />,
                    renderOption
                );

                await page.click(selectors.button);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
