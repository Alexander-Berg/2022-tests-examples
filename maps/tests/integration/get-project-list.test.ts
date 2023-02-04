import * as http from 'http';

import {expect} from 'chai';
import got from 'got';

import {app} from '../../app/app';
import {startServer, stopServer} from './test-server';
import {insertEntry, clearEntries} from './db-fill';

import mockData from './mocks/get-project-list.mock-data';
import * as fixtures from './fixtures/get-project-list';
import defaultGotOptions from './default-got-options';

describe('GET /v1/projects/:project', () => {
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

    it('project1 without limit and offset parameters. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithoutLimitAndOffset);
    });

    it('project2 without limit and offset parameters. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project2`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project2WithoutLimitAndOffset);
    });

    it('project1 with limit. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?limit=50`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithLimit);
    });

    it('offset plus limit partly out of the total count. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?limit=100&offset=150`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.limitAndOffsetPartlyOutOfTotal);
    });

    it('offset out of the total count. should return 200 and empty data', async () => {
        const response = await got.get(`${url}/project1?limit=10&offset=210`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.offsetOutOfTotal);
    });

    it('project1 with order created_at_asc. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?order=created_at_asc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtAsc);
    });

    it('project1 with order created_at_asc and offset. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?offset=50&order=created_at_asc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtAscAndOffset);
    });

    it('project1 with order updated_at_asc. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?order=updated_at_asc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtAsc);
    });

    it('project1 with order updated_at_asc and offset. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?offset=50&order=updated_at_asc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtAscAndOffset);
    });

    it('project1 with order created_at_desc. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?order=created_at_desc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtDesc);
    });

    it('project1 with order created_at_desc and offset. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?offset=50&order=created_at_desc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtDescAndOffset);
    });

    it('project1 with order updated_at_desc. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?order=updated_at_desc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtDesc);
    });

    it('project1 with order updated_at_desc and offset. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?offset=50&order=updated_at_desc`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1OrderedByCreatedAtDescAndOffset);
    });

    it('project1 with created_before condition. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-05-01T13:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.createdBefore);
    });

    it('project1 with created_after condition. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-05-01T13:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.createdAfter);
    });

    it('project1 with updated_before condition. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-05-01T13:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.updatedBefore);
    });

    it('project1 with updated_after condition. should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-05-01T13:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.updatedAfter);
    });

    it('project1 with created_before condition (only date). should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-05-04`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithoutLimitAndOffset);
    });

    it('project1 with created_after condition (only date). should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-04-28`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithoutLimitAndOffset);
    });

    it('project1 with updated_before condition (only date). should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-05-04`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithoutLimitAndOffset);
    });

    it('project1 with updated_after condition (only date). should return 200 and correct answer', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-04-28`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.project1WithoutLimitAndOffset);
    });

    // tslint:disable-next-line: ter-max-len
    it('project1 with created_before condition (only date). should return 200 and correct answer with empty entry list', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-04-28`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.emptyEntryList);
    });

    // tslint:disable-next-line: ter-max-len
    it('project1 with created_after condition (only date). should return 200 and correct answer with empty entry list', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-05-04`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.emptyEntryList);
    });

    // tslint:disable-next-line: ter-max-len
    it('project1 with updated_before condition (only date). should return 200 and correct answer with empty entry list', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-04-28`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.emptyEntryList);
    });

    // tslint:disable-next-line: ter-max-len
    it('project1 with updated_after condition (only date). should return 200 and correct answer with empty entry list', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-05-04`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.emptyEntryList);
    });

    it('project1 with all temporal conditions. should return 200 and correct answer', async () => {
        // tslint:disable-next-line: ter-max-len
        const response = await got.get(`${url}/project1?created_before=2019-05-01T13:30:00Z&created_after=2019-05-01T12:30:00Z&updated_before=2019-05-01T13:15:00Z&updated_after=2019-05-01T12:15:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.allTemporalParameters);
    });

    it('nonexistent project. should return 200 end empty data', async () => {
        const response = await got.get(`${url}/nonexistent`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(200);
        expect(response.body).to.deep.equal(fixtures.nonexistentProject);
    });

    it('too long project name. even if there is such entries in the database. should return 404 code', async () => {
        const response = await got.get(`${url}/${'a'.repeat(256)}`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(404);
    });

    it('incorrect limit. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?limit=qwerty`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('negative limit. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?limit=-10`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('limit is out of range. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?limit=101`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect offset. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?offset=qwerty`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('negative offset. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?offset=-10`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect date at created_before. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-05-32T14:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect date at created_after. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-05-32T14:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect date at updated_before. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-05-32T14:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect date at updated_after. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-05-32T14:00:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect time at created_before. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-12-31T14:62:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect time at created_after. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-12-31T14:62:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect time at updated_before. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-12-31T14:62:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect time at updated_after. should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-12-31T14:62:00Z`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_before (only a year). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_before=2019`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_after (only a year). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_after=2019`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_before (only a year). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_after (only a year). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_before (only a year and a month). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-06`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_after (only a year and a month). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-06`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_before (only a year and a month). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-06`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_after (only a year and a month). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-06`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_before (without seconds). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_before=2019-06-03T14:00`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at created_after (without seconds). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?created_after=2019-06-03T14:00`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_before (without seconds). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_before=2019-06-03T14:00`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });

    it('incorrect format at updated_after (without seconds). should return 400 code', async () => {
        const response = await got.get(`${url}/project1?updated_after=2019-06-03T14:00`, {
            ...defaultGotOptions,
            responseType: 'json'
        });

        expect(response.statusCode).to.equal(400);
    });
});
