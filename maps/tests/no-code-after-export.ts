import noCodeAfterExport from '../rules/no-code-after-export';
import {ruleTester, getError} from './utils/tests-utils';

const validCode = `
const a = 5;
const foo = {};

export let b;
export default a;
export * from foo;
export { a };
`;

const invalidCode = `
const a = true;
const foo = {};

export let b;
export default a;

const b = false;

export * from foo;
export { a };

const c = {
    foo: 'bar'
};
`;

ruleTester.run('no-code-after-export', noCodeAfterExport, {
    valid: [
        {
            code: validCode,
            options: []
        }
    ],
    invalid: [
        {
            code: invalidCode,
            errors: [
                getError(8, 1, 8, 17, 'noCodeAfterExport'),
                getError(13, 1, 15, 3, 'noCodeAfterExport')
            ]
        }
    ]
});
