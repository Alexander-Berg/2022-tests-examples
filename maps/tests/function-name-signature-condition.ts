import FunctionNameSignatureCondition, {Options} from '../rules/function-name-signature-condition';
import {ruleTester, getError} from './utils/tests-utils';

const simpleOptions: Options = [
    [{
        name: 'has.*',
        returnTypes: ['boolean']
    }]
];
const promiseOptions: Options = [
    [{
        name: 'has.*',
        returnTypes: ['boolean']
    }],
    {promiseAllowed: true}
];
const regexOptions: Options = [
    [{
        name: 'is.*',
        returnTypes: [{
            regex: '.* is .*'
        }]
    }]
];
const complexOptions: Options = [[
    {
        name: 'foo.*',
        returnTypes: ['void']
    },
    {
        name: '.*Bar',
        returnTypes: ['boolean', 'number']
    },
    {
        name: 'exact',
        returnTypes: ['React.ReactNode']
    }
]];

const classCode = `
class X {
    hasBar = (): void => {}
    hasFoo(): void {}
    get hasBar(): void {}
}`;
const objectCode = `
const x = {
    hasFoo: (): void => {},
    hasBar: function(): void {}
}`;

ruleTester.run('function-name-signature-condition', FunctionNameSignatureCondition, {
    valid: [
        {
            code: 'function hasFoo(): boolean {}',
            options: simpleOptions
        },
        {
            code: 'function hasFoo(): boolean {}',
            options: promiseOptions
        },
        {
            code: 'function hasFoo(): Promise<boolean> {}',
            options: promiseOptions
        },
        {
            code: 'function isFoo(x): x is Foo {}',
            options: regexOptions
        },
        {
            code: 'function fooMiss() {}',
            options: complexOptions
        },
        {
            code: 'function fooSuccess(): void {}',
            options: complexOptions
        },
        {
            code: 'function successBar(): boolean {}',
            options: complexOptions
        },
        {
            code: 'function anotherSuccessBar(): number {}',
            options: complexOptions
        },
        {
            code: 'function exact(): React.ReactNode {}',
            options: complexOptions
        },
        {
            code: 'function exactNot(): void {}',
            options: complexOptions
        }
    ],
    invalid: [
        {
            code: 'function hasFoo(): void {}',
            options: simpleOptions,
            errors: [getError(1, 10, 1, 16, 'returnTypeError')]
        },
        {
            code: 'function hasFoo(): Promise<boolean> {}',
            options: simpleOptions,
            errors: [getError(1, 10, 1, 16, 'returnTypeError')]
        },
        {
            code: 'const hasFoo = (): void => {}',
            options: simpleOptions,
            errors: [getError(1, 7, 1, 13, 'returnTypeError')]
        },
        {
            code: 'const hasFoo = function x(): void {}',
            options: simpleOptions,
            errors: [getError(1, 7, 1, 13, 'returnTypeError')]
        },
        {
            code: classCode,
            options: simpleOptions,
            errors: [
                getError(3, 5, 3, 11, 'returnTypeError'),
                getError(4, 5, 4, 11, 'returnTypeError'),
                getError(5, 9, 5, 15, 'returnTypeError')
            ]
        },
        {
            code: objectCode,
            options: simpleOptions,
            errors: [
                getError(3, 5, 3, 11, 'returnTypeError'),
                getError(4, 5, 4, 11, 'returnTypeError')
            ]
        },
        {
            code: 'function isFoo(x): boolean {}',
            options: regexOptions,
            errors: [getError(1, 10, 1, 15, 'returnTypeError')]
        },
        {
            code: 'function fooFail(): number {}',
            options: complexOptions,
            errors: [getError(1, 10, 1, 17, 'returnTypeError')]
        },
        {
            code: 'function failBar(): boolean | number {}',
            options: complexOptions,
            errors: [getError(1, 10, 1, 17, 'returnTypeError')]
        },
        {
            code: 'function exact(): void {}',
            options: complexOptions,
            errors: [getError(1, 10, 1, 15, 'returnTypeError')]
        }
    ]
});
