import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { HouseServiceMeterType } from 'types/houseService';

import { HouseServiceCounterSnippet, CounterSnippetType } from '../';

import * as s from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

const counters = Object.values(HouseServiceMeterType);
const counterSnippetType = Object.values(CounterSnippetType);

describe('HouseServiceCounterSnippet', () => {
    counters.forEach((counter) => {
        describe(`Счетчик ${counter}`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(
                        <HouseServiceCounterSnippet
                            type={CounterSnippetType.INFO}
                            houseService={s.getCounter(counter)}
                            onClick={noop}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    counterSnippetType.forEach((type) => {
        describe(`Тип сниппета ${type}`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(
                        <HouseServiceCounterSnippet
                            type={type}
                            houseService={s.getCounter(HouseServiceMeterType.WATER_HOT)}
                            onClick={noop}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
