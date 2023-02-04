import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import accordionStyles from 'view/components/AccordionBlock/styles.module.css';
import { rootReducer } from 'view/entries/manager/reducer';

import { ManagerFlatExcerptsContainer } from '../container';
import excerptsListStyles from '../ManagerFlatExcerptsList/styles.module.css';
import excerptsStyles from '../styles.module.css';

import {
    emptyStore,
    notActiveFlatStatusStore,
    waitingFirstExcerptStore,
    skeletonStore,
    addressErrorStore,
    excerptErrorStore,
    excerptReadyStore,
    excerptReadyWithAtticStore,
    withoutOwnerStore,
    withoutDataStore,
    fewOwnersStore,
    fewSuccessExcerptsStore,
    fewExcerptsWithErrorStore,
    fewExcerptsWithWaitingStore,
    fewSuccessExcerptsWithDifferentAddressesStore,
} from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 900 } }, { viewport: { width: 375, height: 900 } }];

const selectors = {
    lastSuccessExcerpts: `.${excerptsListStyles.lastSuccessExcerpt} .${accordionStyles.title}`,
    requestButton: `.${excerptsStyles.button}`,
    // eslint-disable-next-line max-len
    successExcerpts: `.${excerptsListStyles.EXCERPTS_READY}:not(.${excerptsListStyles.lastSuccessExcerpt}) .${accordionStyles.title}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatExcerptsContainer />
    </AppProvider>
);

describe('ManagerFlatExcerpts', () => {
    describe(`Нет выписок`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={emptyStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Отправлен запрос на получение выписки`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => new Promise(noop),
                };

                await render(<Component store={emptyStore} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.requestButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Ошибка запроса на получение выписки`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => Promise.reject(),
                };

                await render(<Component store={emptyStore} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.requestButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Не активный статус квартиры`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={notActiveFlatStatusStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Ожидается первая выписка`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={waitingFirstExcerptStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Выписка с ошибкой адреса`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={addressErrorStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Выписка с ошибкой при получении`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={excerptErrorStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Готовая выписка`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={excerptReadyStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Готовая выписка с мансардой`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={excerptReadyWithAtticStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Готовая выписка без собственника`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={withoutOwnerStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Готовая выписка без данных `, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={withoutDataStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Готовая выписка с несколькими собственниками `, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={fewOwnersStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Несколько готовых выписок`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={fewSuccessExcerptsStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);
                await page.click(selectors.successExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Несколько готовых выписок с одной ошибкой`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={fewExcerptsWithErrorStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Несколько готовых выписок с ожиданием новой`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={fewExcerptsWithWaitingStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Несколько готовых выписок с разными адресами `, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={fewSuccessExcerptsWithDifferentAddressesStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.lastSuccessExcerpts);
                await page.click(selectors.successExcerpts);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
