import React from 'react';
import { render } from 'jest-puppeteer-react';

import { ContractTermsInfo } from '@vertis/schema-registry/ts-types/realty/rent/api/moderation';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ManagerTermsItem } from '../';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];
const term: ContractTermsInfo = {
    version: 4,
    contractTermsUrl: 'https://yandex.ru/legal/realty_lease_tenant/',
    isPaymentSchemaChanged: true,
    publicationDate: '2022-02-03T00:00:00Z',
    effectiveDate: '2022-02-03T00:00:00Z',
    isSigningRequired: true,
    comment: 'Что-то изменили в соглашениях',
};

describe('ManagerTermsItem', () => {
    describe('Обязательное условие', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<ManagerTermsItem term={term} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Необязательное условие', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<ManagerTermsItem term={{ ...term, isSigningRequired: false }} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
