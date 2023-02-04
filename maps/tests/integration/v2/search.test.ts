import * as nock from 'nock';
import {expect} from 'chai';
import {app} from '../../../app/app';
import {TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';
import {withRecording} from '../nock-utils';
import {createResponseSchemaValidator} from '../response-schema-validator';
import {SearchResults, CompanyProperties, GeometryCollection} from '../../../app/schemas/search/response';

describe('/v2', () => {
    let testServer: TestServer;
    let tvmDaemon: TvmDaemon;

    before(async () => {
        nock.disableNetConnect();
        nock.enableNetConnect();

        [testServer, tvmDaemon] = await Promise.all([
            TestServer.start(app),
            TvmDaemon.start()
        ]);
    });

    after(async () => {
        await Promise.all([
            testServer.stop(),
            tvmDaemon.stop()
        ]);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('GET /v2', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v2'});

        it('should have one digit after comma in "score" in buisiness rating', async () => {
            const res = await withRecording('search/organization', () => {
                return testServer.request('/v2', {
                    query: {
                        lang: 'ru_RU',
                        text: 'Шоколадница',
                        type: 'geo,biz',
                        skip: 0,
                        results: 20,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            result.features.forEach((feature, featureIndex) => {
                const rating = (feature.properties as CompanyProperties).BusinessRating;
                if (rating && rating.score) {
                    const [, fractional] = String(rating.score).split('.');
                    if (fractional) {
                        expect(
                            fractional.length === 1,
                            `Invalid rating.score precision for feature ${featureIndex}`
                        ).to.be.true;
                    }
                }
            });
        });

        it('should have correct response schema for toponym', async () => {
            const res = await withRecording('search/toponym', () => {
                return testServer.request('/v2', {
                    query: {
                        lang: 'ru_RU',
                        text: 'Реутов',
                        type: 'geo,biz',
                        skip: 0,
                        results: 20,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should have response by uri', async () => {
            const res = await withRecording('search/feature-by-uri', () => {
                return testServer.request('/v2', {
                    query: {
                        lang: 'ru_RU',
                        type: 'geo,biz',
                        mode: 'uri',
                        uri: 'ymapsbm1://org?oid=1247572466',
                        skip: 0,
                        results: 20,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            expect(result.features.length > 0).to.be.true;
        });

        it('should have response by business_oid', async () => {
            const res = await withRecording('search/organization-by-business_oid', () => {
                return testServer.request('/v2', {
                    query: {
                        lang: 'ru_RU',
                        type: 'biz',
                        business_oid: '1423973195',
                        skip: 0,
                        results: 10,
                        ask_direct: 1,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            expect(result.features.length === 1).to.be.true;
        });

        it('should have include "MultiLineString" in geometries', async () => {
            const res = await withRecording('search/toponym-multilinestring', () => {
                return testServer.request('/v2', {
                    query: {
                        text: 'невский проспект',
                        lang: 'ru_RU',
                        type: 'geo',
                        results: 1,
                        geometry: 1,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            result.features.forEach((feature) => {
                expect(feature.geometry).not.to.be.undefined;
                expect(feature.geometries).not.to.be.undefined;
                expect(feature.geometries).not.to.be.empty;

                expect(
                    feature.geometries!.find((geometry) => geometry.type === 'MultiLineString')
                ).not.to.be.null;

                expect(
                    feature.geometries!.find((geometry) => geometry.type === 'Point')
                ).not.to.be.null;
            });
        });

        it('should have include "GeometryCollection" in geometries', async () => {
            const res = await withRecording('search/toponym-geometrycollection', () => {
                return testServer.request('/v2', {
                    query: {
                        text: 'москва',
                        lang: 'ru_RU',
                        type: 'geo',
                        results: 1,
                        geometry: 1,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            result.features.forEach((feature) => {
                expect(feature.geometry).not.to.be.undefined;
                expect(feature.geometries).not.to.be.undefined;
                expect(feature.geometries).not.to.be.empty;

                expect(
                    feature.geometries!.find((geometry) => geometry.type === 'Point')
                ).not.to.be.null;

                const geometryCollection = feature.geometries!
                    .find((geometry) => geometry.type === 'GeometryCollection');
                expect(geometryCollection).not.to.be.null;

                (geometryCollection as GeometryCollection).geometries.forEach((geometry) => {
                    expect(geometry.type === 'Polygon').to.be.true;
                });
            });
        });

        it('should have response with type=web', async () => {
            const res = await withRecording('search/organization-with-type-web', () => {
                return testServer.request('/v2', {
                    query: {
                        text: 'кафе',
                        lang: 'ru_RU',
                        type: 'biz,web,geo',
                        skip: 0,
                        results: 10,
                        snippets: 'businessrating/1.x,masstransit/1.x'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            expect(result.features.length > 1).to.be.true;
        });

        it('should have suggest for incorrect request', async () => {
            const res = await withRecording('search/toponym-with-suggest', () => {
                return testServer.request('/v2', {
                    query: {
                        lang: 'ru_RU',
                        text: 'маскв',
                        type: 'geo,biz',
                        skip: 0,
                        results: 20
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const result = res.body as SearchResults;
            const responseMetaData = result.properties.ResponseMetaData;
            expect(responseMetaData).to.not.be.undefined;

            const sourceMetaDataList = responseMetaData!.SearchResponse.SourceMetaDataList;
            expect(sourceMetaDataList).to.not.be.undefined;

            const geocoderMetaData = sourceMetaDataList!.GeocoderResponseMetaData;
            expect(geocoderMetaData).to.not.be.undefined;
            expect(geocoderMetaData!.suggest).to.not.be.undefined;
        });
    });
});
