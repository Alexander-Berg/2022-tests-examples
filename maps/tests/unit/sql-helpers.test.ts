import * as assert from 'assert';
import {generateQueryParametersLine} from '../../server/lib/sql-helpers';

describe('generateQueryParametersLine function', () => {
    describe('The default behavior of the function', () => {
        it('should throw if (3, 2) != ($1, $2), ($3, $4), ($5, $6)', () => {
            assert.equal(generateQueryParametersLine(3, 2), '($1, $2), ($3, $4), ($5, $6)');
        });

        it('should throw if (2, 4) != ($1, $2, $3, $4), ($5, $6, $7, $8)', () => {
            assert.equal(generateQueryParametersLine(2, 4), '($1, $2, $3, $4), ($5, $6, $7, $8)');
        });

        it('should throw if (1, 4) != ($1, $2, $3, $4)', () => {
            assert.equal(generateQueryParametersLine(1, 4), '($1, $2, $3, $4)');
        });
    });

    describe('Function exceptions', () => {
        it('Parameter {offset} must be more than zero', () => {
            assert.throws(
                () => generateQueryParametersLine(1, 4, -1),
                Error,
                'Parameter {offset} must be more than zero'
            );
        });

        it('Parameter {columnCount} must be more than zero', () => {
            assert.throws(
                () => generateQueryParametersLine(2, -1),
                Error,
                'Parameter {columnCount} must be more than zero'
            );
        });

        it('Parameter {recordCount} must be more than zero', () => {
            assert.throws(
                () => generateQueryParametersLine(-1, 2),
                Error,
                'Parameter {recordCount} must be more than zero'
            );
        });
    });
});
