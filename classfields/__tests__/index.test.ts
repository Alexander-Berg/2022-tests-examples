import { getTenantGroupDescriptionType, getTenantGroupTagTypes, getTenantTitle, getTenantGroupTitle } from '../';

import { testCases as descriptionTextTypeTestCases } from './stub/descriptionTextType';
import { testCases as tagTypesTestCases } from './stub/tagTypes';
import { testCases as tenantTitleTestCases } from './stub/tenantTitle';
import { testCases as tenantGroupTitleTestCases } from './stub/tenantGroupTitle';

describe('Проверяем тип группы', () => {
    descriptionTextTypeTestCases.forEach((testCase) => {
        it(testCase.expect, () => {
            const { descriptionType } = getTenantGroupDescriptionType(testCase.data);
            expect(descriptionType).toEqual(testCase.expect);
        });
    });
});

describe('Проверяем тип тэга', () => {
    tagTypesTestCases.forEach((testCase) => {
        it(testCase.expect.toString(), () => {
            const tagTypes = getTenantGroupTagTypes(testCase.data);
            expect(tagTypes).toEqual(testCase.expect);
        });
    });
});

describe('Имя жильца с возрастом', () => {
    tenantTitleTestCases.forEach((testCase) => {
        it(testCase.expect.toString(), () => {
            const tagTypes = getTenantTitle(testCase.data);
            expect(tagTypes).toEqual(testCase.expect);
        });
    });
});

describe('Имя группы', () => {
    tenantGroupTitleTestCases.forEach((testCase) => {
        it(testCase.expect.toString(), () => {
            const tagTypes = getTenantGroupTitle(testCase.data);
            expect(tagTypes).toEqual(testCase.expect);
        });
    });
});
