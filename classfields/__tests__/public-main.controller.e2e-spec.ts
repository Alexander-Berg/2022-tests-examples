import 'jest-extended';
import request from 'supertest';
import { NestExpressApplication } from '@nestjs/platform-express';

import { createTestingApp } from '../../../tests/app';
import { FactoryService } from '../../../services/factory.service';
import { Service } from '../../../types/common';
import { getFixtures } from '../../../tests/get-fixtures';
import { PostStatus } from '../../../types/post';
import { IReadMoreBlock, ISectionBlock } from '../../../types/category-block';

import { fixtures } from './public-main-controller.fixtures';

const DATE_NOW = '2021-11-26T12:12:12.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Public main controller', () => {
    let app: NestExpressApplication;
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

    describe('GET /main/:service', function () {
        it('Возвращает пустой объект, если главная не найдена', async () => {
            const REALTY_SERVICE = Service.realty;

            const response = await request(server).get(`/main/${REALTY_SERVICE}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({});
        });

        it('Возвращает модель главной', async () => {
            const MAIN_DATA = {
                service: Service.realty,
                blocks: [],
            };

            await factory.createMain('Main', MAIN_DATA);

            const response = await request(server).get(`/main/${MAIN_DATA.service}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает модель главной с корректно трансформированными блоками, дополненными постами', async () => {
            const REALTY_SERVICE = Service.realty;
            const {
                MAIN_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                TAG_ATTRIBUTES,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
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
            const contextCategoryPosts = await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);
            const contextTagPosts = await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            ]);

            const contextCategory = await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('posts', contextCategoryPosts);
                return model;
            });
            const contextTag = await factory.createTag('Tag', TAG_ATTRIBUTES).then(async model => {
                await model.$add('posts', contextTagPosts);
                return model;
            });

            const SECTION_BLOCK: ISectionBlock = {
                type: 'section',
                section: {
                    categories: [contextCategory.urlPart],
                    blocks: [
                        {
                            type: 'two',
                            two: [null, contextCategoryPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: contextCategoryPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    tag: contextTag.urlPart,
                    posts: [null, contextCategoryPosts[2]?.urlPart || null, null],
                },
            };

            const MAIN_DATA = {
                ...MAIN_ATTRIBUTES,
                blocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            const [mainTag1, mainTag2] = await factory.createManyTags('Tag', 2, [TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            await factory.createMain('Main', MAIN_DATA);
            await factory.createManyMainTag('MainTag', 2, [
                { order: 1, tag_key: mainTag1?.urlPart },
                { order: 2, tag_key: mainTag2?.urlPart },
            ]);

            const response = await request(server).get(`/main/${REALTY_SERVICE}`).query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });

    describe('GET /main/:service/draft', function () {
        it('Возвращает пустой объект, если черновик главной не найден', async () => {
            const REALTY_SERVICE = Service.realty;

            const response = await request(server).get(`/main/${REALTY_SERVICE}/draft`);

            expect(response.body).toEqual({});
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает черновик главной с корректно трансформированными блоками, дополненными постами', async () => {
            const REALTY_SERVICE = Service.realty;
            const {
                MAIN_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                TAG_ATTRIBUTES,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
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
            const contextCategoryPosts = await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);
            const contextTagPosts = await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
                POST_ATTRIBUTES_9,
                POST_ATTRIBUTES_10,
            ]);

            const contextCategory = await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('posts', contextCategoryPosts);
                return model;
            });
            const contextTag = await factory.createTag('Tag', TAG_ATTRIBUTES).then(async model => {
                await model.$add('posts', contextTagPosts);
                return model;
            });

            const SECTION_BLOCK: ISectionBlock = {
                type: 'section',
                section: {
                    categories: [contextCategory.urlPart],
                    blocks: [
                        {
                            type: 'two',
                            two: [null, contextCategoryPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: contextCategoryPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    tag: contextTag.urlPart,
                    posts: [null, contextCategoryPosts[2]?.urlPart || null, null],
                },
            };

            const MAIN_DATA = {
                ...MAIN_ATTRIBUTES,
                draftBlocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            const [mainTag1, mainTag2] = await factory.createManyTags('Tag', 2, [TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            await factory.createMain('Main', MAIN_DATA);
            await factory.createManyMainTag('MainTag', 2, [
                { order: 1, tag_key: mainTag1?.urlPart },
                { order: 2, tag_key: mainTag2?.urlPart },
            ]);

            const response = await request(server)
                .get(`/main/${REALTY_SERVICE}/draft`)
                .query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });
});
