import * as http from 'http';
import got from 'got';
import {expect} from 'chai';

import {app} from '../../app/app';

import {insertEntry, clearEntries} from './db-fill';
import {startServer, stopServer} from './test-server';
import mock from './mocks/delete-entry.mock-data';
import defaultGotOptions from './default-got-options';

describe('DELETE v1/projects/:project/entries/:key', () => {
    let server: http.Server;
    let url: string;

    before(async () => {
        [server, url] = await startServer(app);
        url += '/v1/projects';

        const entries: any = mock.entries;

        for (const entry of entries) {
            await insertEntry({...entry});
        }
    });

    after(async () => {
        await stopServer(server);
        await clearEntries();
    });

    it('should correctly delete an entry', async () => {
        const response = await got.delete(`${url}/project1/entries/1`, defaultGotOptions);
        const checkDeleted = await got.get(`${url}/project1/entries/1`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(204);
        expect((checkDeleted as any).body.deleted_at).not.null;
    });

    it('should correctly delete hardly an entry', async () => {
        const response = await got.delete(`${url}/project1/entries/1?hard=1`, defaultGotOptions);
        const checkDeleted = await got.get(`${url}/project1/entries/1`, defaultGotOptions);

        expect(checkDeleted.statusCode).to.equal(404);
        expect(response.statusCode).to.equal(204);
    });

    it('should return 404. there is no entries with such key', async () => {
        const response = await got.delete(`${url}/project1/entries/3`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. there is no entries with such key for hard delete', async () => {
        const response = await got.delete(`${url}/project1/entries/3?hard=1`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. there is no entry with such project name', async () => {
        const response = await got.delete(`${url}/project100/entries/1`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. to long project name', async () => {
        const response = await got.delete(`${url}/${'p'.repeat(256)}/entries/1`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404. to long key', async () => {
        const response = await got.delete(`${url}/project2/entries/${'1'.repeat(256)}`, defaultGotOptions);

        expect(response.statusCode).to.equal(404);
    });
});
