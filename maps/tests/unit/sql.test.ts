import * as assert from 'assert';
import {generateQueryParametersLine} from '../../app/lib/sql';

describe('generateQueryParametersLine function', () => {
    describe('The default behavior of the function', () => {
        it('should generate 3 rows of 2 elements', () => {
            assert.strictEqual(generateQueryParametersLine(3, 2), '($1, $2), ($3, $4), ($5, $6)');
        });

        it('should generate 2 rows of 4 elements', () => {
            assert.strictEqual(generateQueryParametersLine(2, 4), '($1, $2, $3, $4), ($5, $6, $7, $8)');
        });

        it('should generate 1 row of 4 elements', () => {
            assert.strictEqual(generateQueryParametersLine(1, 4), '($1, $2, $3, $4)');
        });

        it('should generate 1 row with types', () => {
            assert.strictEqual(generateQueryParametersLine(1, 3, 0, ['text', 'bigint', 'timestamptz']),
                '($1::text, $2::bigint, $3::timestamptz)');
        });

        it('should generate 1 row of 4 elements starting from the 2nd index', () => {
            assert.strictEqual(generateQueryParametersLine(1, 4, 1), '($2, $3, $4, $5)');
        });
    });

    describe('Function exceptions', () => {
        it('should throw if the row count is less than zero', () => {
            assert.throws(() => generateQueryParametersLine(-1, 2, 1));
        });

        it('should throw if the column count is less than zero', () => {
            assert.throws(() => generateQueryParametersLine(2, -1, 1));
        });

        it('should throw if the offset is less than zero', () => {
            assert.throws(() => generateQueryParametersLine(1, 4, -1));
        });

        it('should throw if the types array size doesn\'t match the columns count', () => {
            assert.throws(() => generateQueryParametersLine(1, 3, 1, ['text', 'bigint']));
        });
    });
});
