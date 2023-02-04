import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';
import { IUniversalStore } from 'view/modules/types';

import { IPersonalDataChecksProps, PersonalDataChecks } from '../index';

const renderOptions = [
    { viewport: { width: 1024, height: 812 }, isMobile: false },
    { viewport: { width: 360, height: 568 }, isMobile: true },
];

const initialState: DeepPartial<IUniversalStore> = {
    modal: {},
};

const Component = (props: IPersonalDataChecksProps) => (
    <AppProvider rootReducer={rootReducer} initialState={initialState}>
        <div style={{ backgroundColor: 'grey', padding: '10px' }}>
            <PersonalDataChecks {...props} />
        </div>
    </AppProvider>
);

const testCases: { name: string; prop: IPersonalDataChecksProps }[] = [
    {
        name: 'Базовый рендер',
        prop: {},
    },
    {
        name: 'Есть в базе ФССП с от и до',
        prop: {
            fsspDeptRange: {
                __typename: 'RangeBetween',
                to: 1000000,
                from: 500000,
            },
        },
    },
    {
        name: 'Есть в базе ФССП до',
        prop: {
            fsspDeptRange: {
                __typename: 'RangeTo',
                to: 1000000,
            },
        },
    },
    {
        name: 'Есть в базе ФССП от',
        prop: {
            fsspDeptRange: {
                __typename: 'RangeFrom',
                from: 5000,
            },
        },
    },
];

describe('PersonalDataChecks', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            testCases.forEach(({ name, prop }) => {
                it(`${name} ${renderOption.viewport.width}px`, async () => {
                    await render(<Component {...prop} />, renderOption);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
