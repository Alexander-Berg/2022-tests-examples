import React from 'react';

import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/manager/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import { CianFields } from 'view/modules/managerFlatVasForm/types';
import ModalDisplay from 'view/components/ModalDisplay';

import { ManagerFlatPublishingCianVasFormContainer } from '../container';
import styles from '../styles.module.css';

import { baseStore, filledStore, mobileStore } from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    sendSubmit: `.${styles.cianVasFormSumbit}.Button`,
    inputs: Object.values(CianFields).reduce((acc, field) => {
        acc[field] = `#${field}`;

        return acc;
    }, {} as Record<CianFields, string>),
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: Object.values(CianFields).reduce((acc, field) => {
            acc[field] = `.${stepByStepModalStyles.modal} #${field}`;

            return acc;
        }, {} as Record<CianFields, string>),
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatPublishingCianVasFormContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('ManagerFlatPublishingCianVasForm', () => {
    describe('Заполнение формы', () => {
        it('Заполнение всех полей', async () => {
            await render(<Component store={baseStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            const select = await page.$(selectors.inputs.VAS_TYPE);

            await select?.focus();
            await select?.press('Enter');
            await select?.press('ArrowDown');
            await select?.press('ArrowDown');
            await select?.press('Enter');

            const vasHighlight = await page.$(selectors.inputs.VAS_HIGHLIGHT);
            await vasHighlight?.focus();
            await vasHighlight?.press('Space');

            await page.type(selectors.inputs.AUCTION_BET, '15');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Заполнение всех полей через пошаговость', async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.VAS_TYPE);

            const select = await page.$(selectors.stepByStep.inputs.VAS_TYPE);
            await select?.focus();
            await select?.press('Enter');
            await select?.press('ArrowDown');
            await select?.press('Enter');

            await page.click(selectors.stepByStep.right);
            const vasHighlight = await page.$(selectors.stepByStep.inputs.VAS_HIGHLIGHT);
            await vasHighlight?.focus();
            await vasHighlight?.press('Space');

            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.inputs.AUCTION_BET, '15');
            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={filledStore} Gate={Gate} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Не удалось сохранить форму', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_vas': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={filledStore} Gate={Gate} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Успешное сохранение формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_vas': {
                        return Promise.resolve({});
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={filledStore} Gate={Gate} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
