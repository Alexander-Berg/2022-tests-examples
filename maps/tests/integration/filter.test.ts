import * as http from 'http';
import * as qs from 'querystring';
import {expect} from 'chai';
import got from 'got';

import {app} from '../../app/app';
import {startServer, stopServer} from './test-server';
import {insertEntry, clearEntries} from './db-fill';
import defaultGotOptions from './default-got-options';
import mockData from './mocks/filter.mock-data';
import {correctValue} from './fixtures/filter';

describe('GET /v1/projects/filter', () => {
    let server: http.Server;
    let url: string;

    before(async () => {
        [server, url] = await startServer(app);
        url += '/v1/projects';

        const {entries} = mockData;
        for (const entry of entries) {
            await insertEntry(entry);
        }
    });

    after(async () => {
        await stopServer(server);
        await clearEntries();
    });

    it('should return entries with list of projects by key', async () => {
        const queryParameters = qs.stringify({
            key: 'abc',
            projects: ['project1', 'project2', 'project3'].join(',')
        });

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(correctValue);
    });

    it('should return entry for current project', async () => {
        const queryParameters = qs.stringify({
            key: 'abc',
            projects: 'project1'
        });
        const currentCorrectResult = {
            entries: correctValue.entries.filter(({project}) => project === 'project1')
        };

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(currentCorrectResult);
    });

    it('should return existance entries for list of projects', async () => {
        const queryParameters = qs.stringify({
            key: 'abc',
            projects: ['project1', 'project2', 'project3', 'project4'].join(',')
        });

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(correctValue);
    });

    it('should return 400 code. empty querry string', async () => {
        const response = await got.get(url, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400 code. missing key parameter', async () => {
        const queryParameters = qs.stringify({
            projects: ['project1', 'project2'].join(',')
        });

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 404 code. no such projects for this key', async () => {
        const queryParameters = qs.stringify({
            key: 'abc',
            projects: 'project228'
        });

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(404);
    });

    it('should return 404 code. to long key', async () => {
        const queryParameters = qs.stringify({
            key: 'a'.repeat(256),
            projects: ['project1', 'project2', 'project3'].join(',')
        });

        const response = await got.get(`${url}?${queryParameters}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(404);
    });
});
