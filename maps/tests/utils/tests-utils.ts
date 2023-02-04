import {TSESLint} from '@typescript-eslint/experimental-utils';
import * as path from 'path';

const rootDir = path.join(__dirname, '../../../');
export const ruleTester: TSESLint.RuleTester = new TSESLint.RuleTester({
    parser: path.resolve(rootDir, './node_modules/@typescript-eslint/parser'),
    parserOptions: {
        sourceType: 'module',
        ecmaVersion: 2015
    }
});

export function getError(
    line: number,
    column: number,
    endLine: number,
    endColumn: number,
    messageId: string
): TSESLint.TestCaseError<string> {
    return {
        line,
        column,
        endLine,
        endColumn,
        messageId
    };
}
