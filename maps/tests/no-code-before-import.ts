import noCodeBeforeImport from '../rules/no-code-before-import';
import {ruleTester, getError} from './utils/tests-utils';

const validCode = `
import Foo from './Foo';
function c () {
    const foo = import('foo');
}
import {Foo} from './Foo';

fn();
`;

const codeWithMisplacedDynamicImports = `
import Foo from './Foo';
import {Foo} from './Foo';

console.log();

const foo = import('foo');
function c () {
    const foo = import('foo');
}
function b () {
    const foo = () => {
        d = import('foo')
    }
}
`;

const invalidCode = `
import Foo from './Foo';

console.log();

import x = require('something');

console.log();

function d () {
    const e = () => {
        const foo = import('foo');
    }
}

console.log();
`;

ruleTester.run('no-code-before-import', noCodeBeforeImport, {
    valid: [
        {
            code: validCode,
            options: []
        },
        {
            code: validCode,
            options: [{
                ignoreDynamicImports: false
            }]
        },
        {
            code: codeWithMisplacedDynamicImports,
            options: [{
                ignoreDynamicImports: true
            }]
        }
    ],
    invalid: [
        {
            code: codeWithMisplacedDynamicImports,
            errors: [
                getError(5, 1, 5, 15, 'noCodeBeforeImport')
            ]
        },
        {
            code: invalidCode,
            errors: [
                getError(4, 1, 4, 15, 'noCodeBeforeImport'),
                getError(8, 1, 8, 15, 'noCodeBeforeImport')
            ]
        }
    ]
});
