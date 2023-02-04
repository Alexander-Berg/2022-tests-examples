import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { PostStatus } from '../../../types/post';
import { createTestingApp } from '../../../tests/app';
import { ISectionBlock, IReadMoreBlock } from '../../../types/tag-block';
import { getFixtures } from '../../../tests/get-fixtures';

import { fixtures } from './public-tag-controller.fixtures';

const DATE_NOW = '2021-11-26T12:12:12.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Public tags controller', () => {
    let app: INestApplication;
    let factory: FactoryService;
    let server;

    beforeEach(async () => {
        const testingApp = await createTestingApp();

        app = testingApp.app;
        factory = testingApp.factory;
        await app.init();

        server = app.getHttpServer();
    });

    afterEach(async () => {
        await app.close();
    });

    describe('GET /tags/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag-id';

            const response = await request(server).get(`/tags/${NON_EXISTING_TAG_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Возвращает тег', async () => {
            const { TAG_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createTag('Tag', TAG_ATTRIBUTES);

            const response = await request(server).get(`/tags/${TAG_ATTRIBUTES.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает тег с корректным количеством постов', async () => {
            const { TAG_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.publish }]);
            const tagPosts = await factory.createManyPosts('Post', 3, [
                { status: PostStatus.publish },
                { status: PostStatus.publish },
                { status: PostStatus.draft },
            ]);

            await factory.createTag('Tag', TAG_ATTRIBUTES).then(async model => {
                await model.$add('posts', tagPosts);
                return model;
            });

            const response = await request(server).get(`/tags/${TAG_ATTRIBUTES.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает тег с корректно трансформированными блоками, дополненными постами', async () => {
            const {
                TAG_ATTRIBUTES,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.draft }]);
            const tagPosts = await factory.createManyPosts('Post', 10, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            ]);

            const SECTION_BLOCK: ISectionBlock = {
                type: 'section',
                section: {
                    blocks: [
                        {
                            type: 'two',
                            two: [null, tagPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: tagPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    posts: [null, tagPosts[2]?.urlPart || null, null],
                },
            };

            const TAG_DATA = {
                ...TAG_ATTRIBUTES,
                blocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            await factory.createTag('Tag', TAG_DATA).then(async model => {
                await model.$add('posts', tagPosts);
                return model;
            });

            const response = await request(server).get(`/tags/${TAG_DATA.urlPart}`).query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });

    describe('GET /tags/:urlPart/draft', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag-id';

            const response = await request(server).get(`/tags/${NON_EXISTING_TAG_ID}/draft`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Возвращает черновик тега', async () => {
            const { TAG_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createTag('Tag', TAG_ATTRIBUTES);

            const response = await request(server).get(`/tags/${TAG_ATTRIBUTES.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик тега с корректным количеством постов', async () => {
            const { TAG_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.publish }]);
            const tagPosts = await factory.createManyPosts('Post', 3, [
                { status: PostStatus.publish },
                { status: PostStatus.publish },
                { status: PostStatus.draft },
            ]);

            await factory.createTag('Tag', TAG_ATTRIBUTES).then(async model => {
                await model.$add('posts', tagPosts);
                return model;
            });

            const response = await request(server).get(`/tags/${TAG_ATTRIBUTES.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик тега с корректно трансформированными блоками, дополненными постами', async () => {
            const {
                TAG_ATTRIBUTES,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.draft }]);
            const tagPosts = await factory.createManyPosts('Post', 10, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            ]);

            const SECTION_BLOCK: ISectionBlock = {
                type: 'section',
                section: {
                    blocks: [
                        {
                            type: 'two',
                            two: [null, tagPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: tagPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    posts: [null, tagPosts[2]?.urlPart || null, null],
                },
            };

            const TAG_DATA = {
                ...TAG_ATTRIBUTES,
                draftBlocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            await factory.createTag('Tag', TAG_DATA).then(async model => {
                await model.$add('posts', tagPosts);
                return model;
            });

            const response = await request(server).get(`/tags/${TAG_DATA.urlPart}`).query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });
});
