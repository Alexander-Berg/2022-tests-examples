import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';
import confirmationModalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';

import { RoommatesContainer } from '../container';
import userSnippetStyles from '../RoommatesUserSnippet/styles.module.css';

import { baseStore, skeletonStore, withRoommatesStore, withBackButtonStore, withOneRommateStore } from './stubs';

const renderOptions = [
    { viewport: { width: 1024, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 375, height: 1200 } },
];

const selectors = {
    deleteButton: `.${userSnippetStyles.button}`,
    confirmButton: `.${confirmationModalStyles.button}`,
};

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
}> = ({ store, Gate }) => (
    <div>
        <AppProvider
            initialState={store}
            Gate={Gate}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <RoommatesContainer />
            <ModalDisplay />
        </AppProvider>
    </div>
);

describe('RoommatesContainer', () => {
    describe(`Базовый рендеринг`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={baseStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('есть кнопка возврата', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={withBackButtonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`статусы проверки анкеты сожильца`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={withRoommatesStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Удаление сожителя', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'user.delete_roommate': {
                        return Promise.resolve({
                            users: [],
                        });
                    }
                }
            },
        };
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={withOneRommateStore} Gate={Gate} />, renderOption);

                await page.click(selectors.deleteButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.confirmButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
