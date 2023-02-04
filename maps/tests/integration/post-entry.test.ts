import {expect} from 'chai';
import * as http from 'http';
import * as pg from 'pg';
import got from 'got';

import {app} from '../../app/app';
import * as db from '../../app/lib/db';
import {queryBuilder} from '../../app/lib/query-builder';
import {Entry} from '../../app/typings';
import {clearEntries} from './db-fill';
import {startServer, stopServer} from './test-server';
import defaultGotOptions from './default-got-options';

const gotOptions = {
    ...defaultGotOptions,
    headers: {
        'Content-Type': 'application/json'
    }
};

describe('POST /v1/projects/:project/entries/:key', () => {
    let server: http.Server;
    let url: string;

    before(async () => {
        [server, url] = await startServer(app);
        url += '/v1/projects';
    });

    after(async () => {
        await stopServer(server);
        await clearEntries();
    });

    afterEach(async () => {
        await clearEntries();
    });

    it('should correctly insert an entry', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    foo: 0
                }
            })
        });

        expect(response.statusCode).to.equal(200);

        await getEntry(project, key);
    });

    it('should return code 400. incorrect json data', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: '{"foo": 0'
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. body cannot be an array', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: [{
                    foo: 0
                }]
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. body cannot be a number', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: '123'
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. body cannot be a boolean value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: 'true'
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. body cannot be a null value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: 'null'
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. value cannot be a number', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: 123
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. value cannot be a boolean value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: true
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 400. value cannot be a null value', async () => {
        const project = 'project1';
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: null
            })
        });

        expect(response.statusCode).to.equal(400);

        await checkEntryAbsence(project, key);
    });

    it('should return code 409. two serial requests with similar project and key', async () => {
        const project = 'project1';
        const key = '1000';

        const firstResponse = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    insertion_number: 1
                }
            })
        });

        expect(firstResponse.statusCode).to.equal(200);

        const secondResponse = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    insertion_number: 2
                }
            })
        });

        expect(secondResponse.statusCode).to.equal(409);

        const entry = await getEntry(project, key);

        if (!entry || entry.data.insertion_number !== 1) {
            throw new Error('Incorrect DB insert');
        }
    });

    it('should return 404 code. too long project name', async () => {
        const project = 'a'.repeat(256);
        const key = '1000';

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    foo: 0
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkEntryAbsence(project, key);
    });

    it('should return 404 code. too long entry key', async () => {
        const project = 'project1';
        const key = 'a'.repeat(256);

        const response = await got.post(`${url}/${project}/entries/${key}`, {
            ...gotOptions,
            body: JSON.stringify({
                value: {
                    foo: 0
                }
            })
        });

        expect(response.statusCode).to.equal(404);

        await checkEntryAbsence(project, key);
    });
});

async function getEntry(project: string, key: string): Promise<Entry> {
    const selectQuery: string = queryBuilder
        .select('project', 'key', 'data')
        .from('entries')
        .where({project, key})
        .toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount === 0) {
        throw new Error('Incorrect DB insert');
    }

    return selectResult.rows[0];
}

async function checkEntryAbsence(project: string, key: string): Promise<boolean> {
    const selectQuery: string = queryBuilder
        .select('project', 'key', 'data')
        .from('entries')
        .where({project, key})
        .toString();

    const selectResult: pg.QueryResult = await db.executeReadQuery(selectQuery);

    if (selectResult.rowCount > 0) {
        throw new Error('Incorrect DB insert');
    }

    return true;
}
