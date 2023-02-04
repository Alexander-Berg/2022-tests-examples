import * as pg from 'pg';
import {expect} from 'chai';
import {BaseError} from 'src/lib/base-error';
import {expectRejection} from 'tests/assertions';
import {
    executeInTransaction,
    executeModifyQuery,
    executeReadQuery
} from 'src/lib/db';

describe('Connection', () => {
    describe('withTransaction()', () => {
        class CustomError extends BaseError {}

        async function insertData(client: pg.PoolClient) {
            await client.query('INSERT INTO test_data (value) VALUES (1)');
            await client.query('INSERT INTO test_data (value) VALUES (2)');
        }

        beforeEach(async () => {
            await executeModifyQuery('DROP TABLE IF EXISTS test_data');
            await executeModifyQuery('CREATE TABLE test_data (value integer)');
        });

        afterEach(async () => {
            await executeModifyQuery('DROP TABLE test_data');
        });

        it('should reject promise if body throws error', async () => {
            const trPromise = executeInTransaction(() => {
                throw new CustomError('Inside transaction');
            });
            await expectRejection(trPromise, (err) => {
                expect(err).to.be.instanceof(CustomError);
                expect(err.message).to.equal('Inside transaction');
            });
        });

        describe('if body returns rejected promise', () => {
            let trPromise: Promise<pg.QueryResult>;

            beforeEach(() => {
                trPromise = executeInTransaction(async (client) => {
                    await insertData(client);
                    throw new CustomError('Inside transaction');
                });
            });

            it('should reject promise with body\'s error', async () => {
                await expectRejection(trPromise, (err) => {
                    expect(err).to.be.instanceof(CustomError);
                    expect(err.message).to.equal('Inside transaction');
                });
            });

            it('should rollback transaction', async () => {
                await expectRejection(trPromise);
                const result = await executeReadQuery('SELECT * FROM test_data');
                expect(result.rowCount).to.equal(0);
            });
        });

        describe('if body returns fulfilled promise', () => {
            let trPromise: Promise<string>;

            beforeEach(() => {
                trPromise = executeInTransaction(async (client) => {
                    await insertData(client);
                    return 'ok';
                });
            });

            it('should fulfill promise with body\'s result', async () => {
                const value = await trPromise;
                expect(value).to.equal('ok');
            });

            it('should commit transaction', async () => {
                await trPromise;
                const result: any = await executeReadQuery('SELECT value FROM test_data');
                expect(result.rowCount).to.equal(2);
                expect(result.rows[0].value).to.equal(1);
                expect(result.rows[1].value).to.equal(2);
            });
        });

        it('should not execute queries after transaction rollback', async () => {
            const trPromise = executeInTransaction(async (client) => {
                await insertData(client);
                throw new Error('Second operation failed');
            });
            await expectRejection(trPromise);

            const result = await executeReadQuery('SELECT * FROM test_data');
            expect(result.rowCount).to.equal(0);
        });
    });
});
