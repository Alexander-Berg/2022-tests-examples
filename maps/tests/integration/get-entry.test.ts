import * as http from 'http';

import {expect} from 'chai';
import got from 'got';

import {app} from '../../app/app';
import {startServer, stopServer} from './test-server';
import {insertEntry, clearEntries} from './db-fill';

import mockData from './mocks/get-entry.mock-data';
import * as fixtures from './fixtures/get-entry';
import defaultGotOptions from './default-got-options';

describe('GET /v1/projects/:project/entries/:key', () => {
    let server: http.Server;
    let url: string;

    before(async () => {
        [server, url] = await startServer(app);
        url += '/v1/projects';

        const entries: any[] = mockData.entries;

        for (const entry of entries) {
            await insertEntry({...entry});
        }
    });

    after(async () => {
        await stopServer(server);
        await clearEntries();
    });

    it('should return an entry data', async () => {
        const response = await got.get(`${url}/project1/entries/1000`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.correctEntry);
    });

    it('should return 404 code. there is no entry with such key', async () => {
        const response = await got.get(`${url}/project1/entries/2000`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404 code. there is no entry with such project name', async () => {
        const response = await got.get(`${url}/project2/entries/1000`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404 code. too long project name. even if there is such entry in the database', async () => {
        const response = await got.get(`${url}/${'a'.repeat(256)}/entries/1000`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404 code. too long entry key. even if there is such entry in the database', async () => {
        const response = await got.get(`${url}/project1/entries/${'a'.repeat(256)}`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });
});
