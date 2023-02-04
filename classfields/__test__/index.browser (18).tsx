import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PassportDocumentType } from 'realty-core/app/graphql/operations/types';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';
import { IUniversalStore } from 'view/modules/types';

import { IPersonalDataVerificationStatusProps, PersonalDataVerificationStatus } from '../index';
import { PassportVerificationStatusType } from '../types';

const renderOptions = [
    { viewport: { width: 1024, height: 812 }, isMobile: false },
    { viewport: { width: 360, height: 568 }, isMobile: true },
];

const initialState: DeepPartial<IUniversalStore> = {
    modal: {},
};

const Component = (props: IPersonalDataVerificationStatusProps) => (
    <AppProvider rootReducer={rootReducer} initialState={initialState}>
        <div style={{ backgroundColor: 'grey', padding: '10px' }}>
            <PersonalDataVerificationStatus {...props} />
        </div>
    </AppProvider>
);

const testCases: Record<PassportVerificationStatusType, IPersonalDataVerificationStatusProps> = {
    NotVerifiedStatus: {
        verificationStatus: {
            __typename: 'NotVerifiedStatus',
        },
    },
    SuccessStatus: {
        verificationStatus: {
            __typename: 'SuccessStatus',
        },
    },
    WarningStatus: {
        verificationStatus: {
            __typename: 'WarningStatus',
            fsspDeptRange: {
                from: 23,
                to: 222,
            },
        },
    },
    DifficultiesStatus: {
        verificationStatus: {
            __typename: 'DifficultiesStatus',
        },
    },
    InProgressStatus: {
        verificationStatus: {
            __typename: 'InProgressStatus',
        },
    },
    BadPhotosQualityStatus: {
        verificationStatus: {
            __typename: 'BadPhotosQualityStatus',
            badDocuments: [PassportDocumentType.SelfieWithPassport],
        },
    },
};

describe('PersonalDataVerificatonStatus', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            Object.values(testCases).forEach((props) => {
                it(`${props.verificationStatus.__typename} ${renderOption.viewport.width}px`, async () => {
                    await render(<Component {...props} />, renderOption);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
