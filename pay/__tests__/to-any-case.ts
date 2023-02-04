/* eslint-disable @typescript-eslint/camelcase */

import { toCamelCase } from '../to-camel-case';
import { toSnakeCase } from '../to-snake-case';

const CAMEL_CASE_OBJ: any = {
    numberProp: 1,
    stringProp: '2',
    nullProp: null,
    undefProp: undefined,
    objectProp: {
        boolProp: true,
        nestedObjectProp: {
            arrayProp: ['1', '2', '3'],
        },
    },
    arrayProp: [
        {
            stringProp: '1',
            booleanProp: false,
        },
        {
            stringProp: '2',
            booleanProp: true,
        },
    ],
};

const SNAKE_CASE_OBJ: any = {
    number_prop: 1,
    string_prop: '2',
    null_prop: null,
    undef_prop: undefined,
    object_prop: {
        bool_prop: true,
        nested_object_prop: {
            array_prop: ['1', '2', '3'],
        },
    },
    array_prop: [
        {
            string_prop: '1',
            boolean_prop: false,
        },
        {
            string_prop: '2',
            boolean_prop: true,
        },
    ],
};

describe('Utils Object', () => {
    describe('to-camel-case', () => {
        it('success: change-case', () => {
            const camelCaseObj = toCamelCase(SNAKE_CASE_OBJ);

            expect(camelCaseObj).toEqual(CAMEL_CASE_OBJ);
        });
    });

    describe('to-snake-case', () => {
        it('success: change-case', () => {
            const snakeCaseObj = toSnakeCase(CAMEL_CASE_OBJ);

            expect(snakeCaseObj).toEqual(SNAKE_CASE_OBJ);
        });
    });
});
