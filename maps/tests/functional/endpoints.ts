import * as fs from 'fs';
import * as got from 'got';
import * as chai from 'chai';
import * as chaiJsonSchema from 'chai-json-schema';
import {app} from '../../src/app';
import {LayerInfo} from '../../src/v2/parse-meta-response';
import {TestServer} from './test-server';
import {withRecording} from '../utils/nock-utils';

const expect = chai.expect;
const apiSpec = JSON.parse(fs.readFileSync('out/generated/v2/spec.json', 'utf8'));

chai.use(chaiJsonSchema);

type SchemaValidator = (response: got.Response<any>) => void;

function createSchemaValidator(path: string, method: string): SchemaValidator {
    const operation = apiSpec.paths[path][method.toLowerCase()];
    expect(operation, `Operation for method ${method}, path ${path} does not exist`).to.exist;
    return (res) => {
        const schema = operation.responses[res.statusCode!].content['application/json'].schema;
        expect(schema).to.exist;
        expect(res.body).to.be.jsonSchema(schema);
    };
}

function expectJsonContentType(response: got.Response<any>): void {
    expect(response.headers['content-type']).to.match(/application\/json/);
}

describe('endpoints', function () {
    this.timeout(5000);

    let server: TestServer;

    before(async () => {
        server = await TestServer.start(app);
    });

    after(async () => {
        await server.stop();
    });

    describe('GET /v2', () => {
        const validateSchema = createSchemaValidator('/v2', 'get');

        function expectLayers(body: LayerInfo[], layers: string[]): void {
            const actualLayers = body.map((layer) => layer.id);
            expect(actualLayers).to.have.members(layers);
        }

        it('should handle many layers', async () => {
            const res = await withRecording('/v2/many-layers', () => {
                return server.request('/v2', {
                    query: {
                        l: 'map,trf,router,sat',
                        ll: '38.90944486,45.08747892',
                        z: 16,
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expectLayers(res.body, ['map', 'trf', 'router', 'sat']);
        });

        it('should handle "geoalias" layer', async () => {
            const res = await withRecording('/v2/geoalias-layer', () => {
                return server.request('/v2', {
                    query: {
                        l: 'geoalias',
                        ll: '37.61767100,55.75576800',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expectLayers(res.body, ['geoalias']);
        });

        it('should skip unknown layers', async () => {
            const res = await withRecording('/v2/unknown-layers', () => {
                return server.request('/v2', {
                    query: {
                        l: 'map,foobar',
                        ll: '37.61767100,55.75576800',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expectLayers(res.body, ['map']);
        });

        it('should proxy bad request from meta backend (MAPSCORE-3933)', async () => {
            const res = await withRecording('/v2/proxy-bad-request', () => {
                return server.request('/v2', {
                    query: {
                        l: 'map,stv',
                        ll: '4.32253776,64.63012364',
                        z: 4,
                        spn: '116.01562500,33.62973166',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.match(/^Access for span request not granted/);
        });
    });

    describe('GET /v2/layers_stamps', () => {
        const validateSchema = createSchemaValidator('/v2/layers_stamps', 'get');

        it('should return info for one layer', async () => {
            const res = await withRecording('/v2/layers_stamps/one-layer', () => {
                return server.request('/v2/layers_stamps', {
                    query: {
                        l: 'hph',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expect(res.body).to.have.keys(['hph']);
        });

        it('should return info for many layers', async () => {
            const res = await withRecording('/v2/layers_stamps/many-layers', () => {
                return server.request('/v2/layers_stamps', {
                    query: {
                        l: 'pht,hph',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expect(res.body).to.have.keys(['pht', 'hph']);
        });

        it('should skip unknown layers', async () => {
            const res = await withRecording('/v2/layers_stamps/unknown-layer', () => {
                return server.request('/v2/layers_stamps', {
                    query: {
                        l: 'pht,foo',
                        lang: 'ru_RU'
                    },
                    json: true
                });
            });
            expect(res.statusCode).to.equal(200);
            expectJsonContentType(res);
            validateSchema(res);
            expect(res.body).to.have.keys(['pht']);
        });
    });
});
