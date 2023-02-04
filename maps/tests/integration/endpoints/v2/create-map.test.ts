import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/maps',
    method: 'post'
});

export const main: EndpointSuiteDefinition = (context) => {
    describe('POST /v2/maps', () => {
        let newMap: any;

        beforeEach(() => {
            newMap = {
                properties: mapFixture.properties,
                options: mapFixture.options,
                state: mapFixture.state
            };
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps', {
                    method: 'POST'
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    method: 'POST',
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should insert new map to database', async () => {
            const res = await context.server.request('/v2/maps', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true,
                body: newMap
            });
            expect(res.statusCode).to.equal(201);

            let result = await context.db.query('SELECT * FROM maps');

            expect(result.rowCount).to.equal(1, 'Invalid number of rows inserted to "maps" table');

            const insertedMap = result.rows[0];
            expect(insertedMap.properties).to.deep.equal(newMap.properties);
            expect(insertedMap.options).to.deep.equal(newMap.options);
            expect(insertedMap.state).to.deep.equal(newMap.state);
            expect(insertedMap.revision).to.equal('0', 'Invalid new map revision');

            result = await context.db.query('SELECT * FROM maps_users');

            expect(result.rowCount).to.equal(
                1,
                'Invalid number of rows inserted to "maps_users" table'
            );

            const mapUser = result.rows[0];
            expect(mapUser.uid).to.equal('1', 'Invalid uid associated with new map');
            expect(mapUser.map_id).to.equal(insertedMap.id, 'Invalid map associated with uid');
            expect(mapUser.role).to.equal('administrator', 'Invalid role for new map');
        });

        it('should return created map in response', async () => {
            const res = await context.server.request('/v2/maps', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true,
                body: newMap
            });
            expect(res.statusCode).to.equal(201);

            const mapsResult = await context.db.query('SELECT * FROM maps');

            const sid = mapsResult.rows[0].sid;

            expect(res.headers.location).to.match(
                new RegExp(`/v2\/maps\/${sid}$`),
                'Invalid Location header value'
            );

            validateResponseSchema(res);
            expect(res.body.sid).to.equal(sid, 'Invalid map in response');
            expect(res.body.revision).to.equal('0', 'Invalid map revision in response');
        });
    });
};
