import {expect} from 'chai';
import * as http from 'http';
import * as pg from 'pg';
import got from 'got';

import {app} from '../../app/app';
import * as db from '../../app/lib/db';
import {queryBuilder} from '../../app/lib/query-builder';
import {Entry} from '../../app/typings';
import {insertEntry, clearEntries} from './db-fill';
import {startServer, stopServer} from './test-server';
import defaultGotOptions from './default-got-options';

import mockData from './mocks/put-entry.mock-data';
const entries: any[] = mockData.entries;

const gotOptions = {
    ...defaultGotOptions,
    headers: {
        'Content-Type': 'application/json'
    }
};

describe('PUT /v1/projects/:project/entries/:key', () => {
    let server: http.Server;
    let url: string;

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

    it('should correctly update an entry', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(200);

        await checkIfEntryUpdated(project, key);
    });

    it('should correctly update an entry with show_deleted parameter', async () => {
        const project = 'project2';
        const key = '2000';

        const response = await got.put(`${url}/${project}/entries/${key}?show_deleted=1`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(200);

        await checkIfEntryUpdated(project, key);
    });

    it('should correctly update an entry with apply_diff parameter', async () => {
        const project = 'project1';
        const key = '1001';
        const oldData = mockData.entries.find((entry) => entry.key === key)!.data;

        const putResponse = await got.put(`${url}/${project}/entries/${key}?apply_diff=1`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        const getResponse = await got.get(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            responseType: 'json'
        });

        const updatedData = (getResponse as any).body.value;

        expect(putResponse.statusCode).to.equal(200);
        expect(updatedData).to.deep.equal({...oldData, new_entry: true});

        await checkIfEntryNotUpdated(project, key);
    });

    it('should correctly update an entry with apply_diff and show_deleted query parameters', async () => {
        const project = 'project2';
        const key = '2000';
        const oldData = mockData.entries.find((entry) => entry.key === key)!.data;

        const putResponse = await got.put(`${url}/${project}/entries/${key}?apply_diff=1&show_deleted=1`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        const getResponse = await got.get(`${url}/${project}/entries/${key}?show_deleted=1`, {
            ...gotOptions,
            responseType: 'json'
        });

        const updatedData = (getResponse as any).body.value;

        expect(putResponse.statusCode).to.equal(200);
        expect(updatedData).to.deep.equal({...oldData, new_entry: true});

        await checkIfEntryUpdated(project, key);
    });

    it('should correctly updaete an entry marked as deleted', async () => {
        const project = 'project2';
        const key = '2000';

        await got.delete(`${url}/${project}/entries/${key}`, gotOptions);

        const updateResponse = await got.put(`${url}/${project}/entries/${key}?show_deleted=1`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(updateResponse.statusCode).to.equal(200);

        await checkIfEntryUpdated(project, key);
    });

    it('should return code 400. incorrect json data', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: '{"foo": 0'
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. value cannot be an array', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: [{
                    new_entry: true
                }]
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. body cannot be a number', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: '123'
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. body cannot be a boolean value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: 'true'
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. body cannot be a null value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: 'null'
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. value cannot be a number', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: 123
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. body cannot be a boolean value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: true
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. body cannot be a null value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: null
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. query parameter must be named only apply_diff', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}?something_else=1000`, {
            ...gotOptions,
            body: JSON.stringify({
                value: null
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. query parameter must be unique apply_diff', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}?apply_diff=1&apply_diff=0`, {
            ...gotOptions,
            body: JSON.stringify({
                value: false
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. query string must include only apply_diff', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}?apply_diff=1&something_else=1000`, {
            ...gotOptions,
            body: JSON.stringify({
                value: false
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. apply diff parameter can be only number', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}?apply_diff=true`, {
            ...gotOptions,
            body: JSON.stringify({
                value: null
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return code 400. apply diff parameter number can be only 0 or 1', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}?apply_diff=2`, {
            ...gotOptions,
            body: JSON.stringify({
                value: null
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return 404 code. too long project name. even if there is such entry in the database', async () => {
        const project = 'a'.repeat(256);
        const key = '1000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return 404 code. too long entry key. even if there is such entry in the database', async () => {
        const project = 'project1';
        const key = 'a'.repeat(256);

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkIfEntryNotUpdated(project, key);
    });

    it('should return 404 code. there is no such entry in the database', async () => {
        const project = 'project1';
        const key = '2002';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkEntryAbsence(project, key);
    });

    it('should return 404 code. can not update entry that is marked as deleted without show_deleted', async () => {
        const project = 'project2';
        const key = '2000';

        const response = await got.put(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should reutrn 404 code. there is no such entry in the database using apply diff', async () => {
        const project = 'project1';
        const key = '2001';

        const response = await got.put(`${url}/${project}/entries/${key}?apply_diff=1`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    new_entry: true
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkEntryAbsence(project, key);
    });
});

async function checkIfEntryUpdated(project: string, key: string): Promise<boolean> {
    const selectQuery: string = queryBuilder
        .select('project', 'key', 'data')
        .from('entries')
        .where({project, key})
        .toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount === 0) {
        throw new Error('Incorrect DB update');
    }

    const entry: Entry = selectResult.rows[0];

    if (!entry.data || !entry.data.new_entry) {
        throw new Error('Incorrect DB update');
    }

    return true;
}

async function checkIfEntryNotUpdated(project: string, key: string): Promise<boolean> {
    const selectQuery: string = queryBuilder
        .select('project', 'key', 'data')
        .from('entries')
        .where({project, key})
        .toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount === 0) {
        throw new Error('Incorrect DB update');
    }

    const entry: Entry = selectResult.rows[0];

    if (!entry.data || !entry.data.old_entry) {
        throw new Error('Incorrect DB update');
    }

    return true;
}

async function checkEntryAbsence(project: string, key: string): Promise<boolean> {
    const selectQuery: string = queryBuilder
        .select('project', 'key', 'data')
        .from('entries')
        .where({project, key})
        .toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount > 0) {
        throw new Error('Incorrect DB update');
    }

    return true;
}
