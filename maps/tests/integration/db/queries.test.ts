import * as pg from 'pg';
import * as chai from 'chai';
import chaiAsPromised from 'chai-as-promised';
import {dbClient} from 'app/lib/db-client';

chai.use(chaiAsPromised);
const {expect} = chai;

describe('Database', () => {
    class CustomError extends Error {}

    const dataToInsert = [1, 2, 3, 4, 5];
    const insertQuery = `INSERT INTO test_data (value) VALUES ${dataToInsert.map((x) => `(${x})`).join(',')}`;

    async function insertData(client: pg.PoolClient) {
        await client.query(insertQuery);
    }

    beforeEach(async () => {
        await dbClient.executeWriteQuery('DROP TABLE IF EXISTS test_data');
        await dbClient.executeWriteQuery('CREATE TABLE test_data (value integer)');
    });

    afterEach(async () => {
        await dbClient.executeWriteQuery('DROP TABLE test_data');
    });

    describe('write query', () => {
        it('should be rejected on the invalid query', async () => {
            const transactionPromise = dbClient.executeWriteQuery('INVALID QUERY');
            await expect(transactionPromise).to.eventually.be.rejected;
        });

        it('should commit transaction', async () => {
            await dbClient.executeWriteQuery(insertQuery);

            const {data} = await dbClient.executeReadQuery('SELECT * FROM test_data');
            expect(data.rowCount).to.equal(dataToInsert.length);
            const actualRows = data.rows.map((row: any) => row.value);
            expect(actualRows).to.deep.equal(dataToInsert);
        });
    });

    describe('write callback', () => {
        it('should be rejected if body throws error', async () => {
            const transactionPromise = dbClient.executeWriteCallback(() => {
                throw new CustomError('Inside modify transaction');
            });
            await expect(transactionPromise)
                .to.eventually.be.rejectedWith('Inside modify transaction')
                .and.be.instanceof(CustomError);
        });

        it('should not execute queries after transaction rollback', async () => {
            const transactionPromise = dbClient.executeWriteCallback(async (client: pg.PoolClient) => {
                await insertData(client);
                throw new Error('Second operation failed');
            });
            await expect(transactionPromise).to.eventually.be.rejected;

            const {data} = await dbClient.executeReadQuery('SELECT * FROM test_data');
            expect(data.rowCount).to.equal(0);
        });

        it('should commit transaction', async () => {
            await dbClient.executeWriteCallback(insertData);

            const {data} = await dbClient.executeReadQuery('SELECT * FROM test_data');
            expect(data.rowCount).to.equal(dataToInsert.length);
            const actualRows = data.rows.map((row: any) => row.value);
            expect(actualRows).to.deep.equal(dataToInsert);
        });
    });

    describe('read query', () => {
        beforeEach(async () => {
            await dbClient.executeWriteQuery(insertQuery);
        });

        it('should be rejected on the invalid query', async () => {
            const transactionPromise = dbClient.executeReadQuery('INVALID QUERY');
            await expect(transactionPromise).to.eventually.be.rejected;
        });

        it('should return data', async () => {
            const {data} = await dbClient.executeReadQuery('SELECT * FROM test_data');
            expect(data.rowCount).to.equal(dataToInsert.length);
            const actualRows = data.rows.map((row: any) => row.value);
            expect(actualRows).to.deep.equal(dataToInsert);
        });
    });

    describe('read callback', () => {
        beforeEach(async () => {
            await dbClient.executeWriteQuery(insertQuery);
        });

        it('should be rejected if body throws error', async () => {
            const transactionPromise = dbClient.executeReadCallback(() => {
                throw new CustomError('Inside modify transaction');
            });
            await expect(transactionPromise)
                .to.eventually.be.rejectedWith('Inside modify transaction')
                .and.be.instanceof(CustomError);
        });

        it('should return data', async () => {
            const {data} = await dbClient.executeReadCallback(async (client: pg.PoolClient) => {
                return client.query('SELECT * FROM test_data');
            });

            expect(data.rowCount).to.equal(dataToInsert.length);
            const actualRows = data.rows.map((row: any) => row.value);
            expect(actualRows).to.deep.equal(dataToInsert);
        });
    });
});
