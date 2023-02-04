import * as http from 'http';
import * as pg from 'pg';
import got from 'got';
import {expect} from 'chai';

import {app} from '../../app/app';
import * as db from '../../app/lib/db';
import {queryBuilder} from '../../app/lib/query-builder';

import {startServer, stopServer} from './test-server';
import {insertEntry, clearEntries} from './db-fill';
import mockData from './mocks/batch-update.mock-data';

const entries: any = mockData.entries;

const defaultGotOptions = {
    retry: 0,
    timeout: 1500,
    throwHttpErrors: false,
    headers: {
        'Content-Type': 'application/json'
    }
};

const dataGenerator = (key: string) => ({
    key,
    data: {
        foo: 'bar'
    }
});

describe('PATCH /v1/projects/:project', () => {
    let server: http.Server;
    let url: string;
    const project = 'project1';

    before(async () => {
        [server, url] = await startServer(app);
        url += '/v1/projects';

        for (const entry of entries) {
            await insertEntry({...entry});
        }
    });

    after(async () => {
        await stopServer(server);
        await clearEntries();
    });

    afterEach(async () => {
        await clearEntries();

        for (const entry of entries) {
            await insertEntry({...entry});
        }
    });

    it('should update the provided entries correctly', async () => {
        const key = '4';
        const value = [
            {
                key,
                data: {
                    new_entry: {
                        first: true
                    }
                }
            }
        ];

        const response = await got.patch(`${url}/${project}`, {
            ...defaultGotOptions,
            body: JSON.stringify({value})
        });

        const updatedFields = await getDbData(project, [key]);
        const transformedData = updatedFields.map(({data, key}) => ({
            key,
            data
        }));

        expect(transformedData).to.deep.equal(value);
        expect(response.statusCode).to.equal(200);
    });

    it('should update the provided entries correctly with apply_diff parameter', async () => {
        const keys = ['1', '2', '3'];
        const value = keys.map(dataGenerator);

        const response = await got.patch(`${url}/${project}?apply_diff=1`, {
            ...defaultGotOptions,
            body: JSON.stringify({value})
        });

        const updatedFields = await getDbData(project, keys);
        const transformedValue = value.reduce((result, {key, data}) => {
            result[key] = {...data};
            return result;
        }, {} as {[key: string]: any});
        const transformedData = updatedFields.map(({key, data}) => ({
            key,
            data: {
                ...data,
                ...transformedValue[key]
            }
        }));

        expect(transformedData).to.deep.equal(updatedFields);
        expect(response.statusCode).to.equal(200);
    });

    it('should update the existance entries correctly by existance parameter', async () => {
        const keys = ['1', '2', '3', '10'];
        const value = keys.map(dataGenerator);

        const response = await got.patch(`${url}/${project}?existance=1`, {
            ...defaultGotOptions,
            body: JSON.stringify({value})
        });

        const updatedFields = await getDbData(project, keys);
        const transformedData = updatedFields.map(({key, data}) => ({
            key,
            data
        }));
        const correctValue = value.filter(({key}) => key !== '10');

        expect(transformedData).to.deep.equal(correctValue);
        expect(response.statusCode).to.equal(200);
    });

    it('should return 400. request without body', async () => {
        const response = await got.patch(`${url}/${project}`, {...defaultGotOptions});

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. incorrect body', async () => {
        const response = await got.patch(`${url}/${project}`, {
            ...defaultGotOptions,
            body: 'foobar'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. incorrect body value', async () => {
        const response = await got.patch(`${url}/${project}`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: [
                    {
                        foo: 'bar',
                        bar: 'foo'
                    }
                ]
            })
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. query must include only apply_diff parameter', async () => {
        const response = await got.patch(`${url}/${project}?smth_else=1`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['1', '2', '3'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. apply_diff must be number 0 or 1', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff=smth_else`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['1', '2', '3'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. apply_diff must be the only one', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff=1&smth_else=2`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['1', '2', '3'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. query must have the one apply_diff parameter without duplicates', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff=1&apply_diff=0`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['1', '2', '3'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400. body with applying diff must be a flat object', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff`, {
            ...defaultGotOptions,
            body: JSON.stringify({value: [{
                4: {
                    old_entry: {
                        second: true
                    }
                }
            }]})
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 404. trying to update a non-existent data', async () => {
        const value = [];

        for (let i = 1; i < 6; i++) {
            value.push(dataGenerator(String(i)));
        }

        const response = await got.patch(`${url}/${project}`, {
            ...defaultGotOptions,
            body: JSON.stringify({value})
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. trying to update a non-existent data with apply_diff parameter', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff=1`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['10'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. check sql injection', async () => {
        const response = await got.patch(`${url}/${project}?apply_diff=1`, {
            ...defaultGotOptions,
            body: JSON.stringify({
                value: ['DROP TABLE entries'].map(dataGenerator)
            })
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. project name too long', async () => {
        const longProjectName = 'a'.repeat(256);
        const response = await got.patch(`${url}/${longProjectName}`, {
            ...defaultGotOptions,
            body: JSON.stringify({value: [dataGenerator(String('1'))]})
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should return 413. request body too large', async () => {
        const value = [];

        for (let i = 0; i < 200; i++) {
            value.push(dataGenerator(String(i)));
        }

        const response = await got.patch(`${url}/${project}`, {
            ...defaultGotOptions,
            body: JSON.stringify({value})
        });

        expect(response.statusCode).to.equal(413);
    });
});

async function getDbData(project: string, keys: string[]): Promise<{project: string, key: string, data: any}[]> {
    const selectQuery: string = queryBuilder.raw(
        `SELECT entries.key, entries.data
        FROM entries INNER JOIN (
            SELECT unnest(array[${keys.map(() => '? ')}]) as key
        ) as tmp_table ON tmp_table.key = entries.key
        WHERE entries.project = ? AND entries.key = tmp_table.key`,
        [...keys, project]
    ).toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount === 0) {
        throw new Error('Incorrect DB update');
    }

    return selectResult.rows;
}
