import * as got from 'got';
import {TestServer} from 'tests/integration/test-server';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {SELF_SERVICE_TICKET, UID1_USER_TICKET} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

export const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/maps/{sid}/objects',
    method: 'patch'
});

export function batchUpdate(server: TestServer, operations: any): Promise<got.Response<any>> {
    return server.request('/v2/maps/sid-1/objects', {
        method: 'PATCH',
        headers: {
            'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
            'X-Ya-User-Ticket': UID1_USER_TICKET
        },
        body: {
            revision: '1',
            operations
        },
        json: true
    });
}

interface MapFields {
    id: number;
    sid: string;
    revision?: string;
    deleted?: boolean;
}

export function createMap({id, sid, revision = '1', deleted = false}: MapFields) {
    return {
        id: id,
        sid: sid,
        time_created: '2016-07-14 01:00:00',
        time_updated: '2016-07-14 01:00:00',
        revision: revision,
        deleted: deleted,
        properties: mapFixture.properties,
        options: mapFixture.options,
        state: mapFixture.state
    };
}
