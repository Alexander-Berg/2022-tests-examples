import AsteriskImport from '../rules/asterisk-import';
import {ruleTester} from './utils/tests-utils';

const options = ['react', 'redux'];
const errors = [{messageId: 'nonAsteriskForbidden'}];

const validSpecifiers = [
    '* as foo'
];
const fixableInvalidSpecifiers = [
    'foo'
];
const unfixableInvalidSpecifiers = [
    '{foo}',
    '{foo as bar}',
    '{foo, bar as baz}',
    '{bar as baz, foo}',
    'abc, {foo}',
    'foo, * as bar'
];

ruleTester.run('asterisk-import', AsteriskImport, {
    valid: [
        ...validSpecifiers.map((specifier) => ({
            code: `import ${specifier} from 'react'`,
            options
        })),
        ...fixableInvalidSpecifiers.concat(unfixableInvalidSpecifiers).map((specifier) => ({
            code: `import ${specifier} from 'other-than-react-etc'`,
            options
        }))
    ],
    invalid: [
        ...fixableInvalidSpecifiers.map((specifier) => ({
            code: `import ${specifier} from 'redux'`,
            options,
            errors
        })),
        ...unfixableInvalidSpecifiers.map((specifier) => ({
            code: `import ${specifier} from 'react'`,
            options,
            errors,
            output: null
        }))
    ]
});
