import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import trashbox from '@realty-front/icons/common/trashbox-24.svg';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { UserPaymentMethodsCardBase } from '../';
import { Skeleton } from '../Skeleton';

import * as s from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

describe('UserPaymentMethodsCardBase', () => {
    describe(`Без иконки`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<UserPaymentMethodsCardBase card={s.tinkoffCard} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Базовое состояние`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <UserPaymentMethodsCardBase icon={trashbox} card={s.tinkoffCard} onIconClick={noop} />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Две карточки рядом`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <React.Fragment>
                        <UserPaymentMethodsCardBase icon={trashbox} card={s.tinkoffCard} onIconClick={noop} />
                        <UserPaymentMethodsCardBase icon={trashbox} card={s.sberbankCard} onIconClick={noop} />
                    </React.Fragment>,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Skeleton />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
