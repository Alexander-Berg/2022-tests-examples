import * as http from 'http';
import * as got from 'got';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import editPartner from 'tests/fixtures/integration/v1/partner/edit-partner.json';
import createPartner from 'tests/fixtures/integration/v1/partner/create-partner.json';
import updatePartner from 'tests/fixtures/integration/v1/partner/update-partner.json';
import createdPartner from 'tests/fixtures/integration/v1/partner/created-partner.json';
import updatedPartner from 'tests/fixtures/integration/v1/partner/updated-partner.json';
import {ResponseBody} from 'tests/types';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/edit', () => {
    const testDb = new TestDb();
    let server: http.Server;
    let url: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
    });

    afterAll(async () => {
        await stopServer(server);
    });

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);
    });

    describe('/v1/edit/partner', () => {
        it('should return partner', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/partner`, {
                searchParams: {
                    id: 1
                }
            });

            expect(statusCode).toEqual(200);
            expect(body).toEqual(editPartner);
        });
    });

    describe('/v1/edit/create_partner', () => {
        it('should create partner', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v1/edit/create_partner`, {
                json: createPartner
            });

            expect(statusCode).toEqual(201);

            const id = body.data.id;
            expect(body).toEqual({
                success: true,
                data: {id}
            });

            const checkResult = await client.get(`${url}/v1/edit/partner`, {
                searchParams: {id}
            });

            expect(checkResult.body).toEqual(createdPartner);
        });
    });

    describe('/v1/edit/update_partner', () => {
        it('should update partner', async () => {
            const createResult = await client.post<ResponseBody>(`${url}/v1/edit/create_partner`, {
                json: createPartner
            });

            expect(createResult.statusCode).toEqual(201);

            const id = createResult.body.data.id;
            const updateResult = await client.post<ResponseBody>(`${url}/v1/edit/update_partner`, {
                json: updatePartner,
                searchParams: {id}
            });

            expect(updateResult.statusCode).toEqual(202);

            const checkResult = await client.get(`${url}/v1/edit/partner`, {
                searchParams: {id}
            });

            expect(checkResult.body).toEqual(updatedPartner);
        });
    });

    describe('/v1/edit/update_partner_branch', () => {
        const id = 1;

        it('should archive partner and linked pages', async () => {
            const {
                body: {data: partnersRawBefore}
            } = await client.get<ResponseBody>(`${url}/v1/edit/partner_list`);
            const partnersBefore = partnersRawBefore.filter((partner: {id: number}) => partner.id === id);
            expect(partnersBefore.length).toBeGreaterThan(0);

            const archiveResult = await client.post<ResponseBody>(`${url}/v1/edit/update_partner_branch`, {
                searchParams: {
                    id,
                    branch: 'archive'
                }
            });
            expect(archiveResult.statusCode).toEqual(202);

            const {
                body: {data: partnersRawAfter}
            } = await client.get<ResponseBody>(`${url}/v1/edit/partner_list`);
            const partnersAfter = partnersRawAfter.filter((partner: {id: number}) => partner.id === id);
            expect(partnersAfter.length).toEqual(0);

            const {
                body: {data: pages}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`);
            expect(pages.find((page: {partnerId: number}) => page.partnerId === id)).toBeUndefined();
        });

        it('should unarchive partner', async () => {
            const archiveResult = await client.post<ResponseBody>(`${url}/v1/edit/update_partner_branch`, {
                searchParams: {
                    id,
                    branch: 'archive'
                }
            });
            expect(archiveResult.statusCode).toEqual(202);

            const {
                body: {data: partnersRawBefore}
            } = await client.get<ResponseBody>(`${url}/v1/edit/partner_list`);
            const partnersBefore = partnersRawBefore.filter((partner: {id: number}) => partner.id === id);
            expect(partnersBefore.length).toEqual(0);

            const publisheResult = await client.post<ResponseBody>(`${url}/v1/edit/update_partner_branch`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(publisheResult.statusCode).toEqual(202);

            const {
                body: {data: partnersRawAfter}
            } = await client.get<ResponseBody>(`${url}/v1/edit/partner_list`);
            const partnersAfter = partnersRawAfter.filter((partner: {id: number}) => partner.id === id);
            expect(partnersAfter.length).toBeGreaterThan(0);

            const {
                body: {data: pages}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`);
            expect(pages.find((page: {partnerId: number}) => page.partnerId === id)).toBeUndefined();
        });

        it('should return 404 on not existed partner', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/update_partner_branch`, {
                searchParams: {
                    id: 10000,
                    branch: 'archive'
                }
            });

            expect(statusCode).toEqual(404);
        });
    });
});
