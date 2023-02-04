import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { PostStatus } from '../../../types/post';
import { createTestingApp } from '../../../tests/app';
import { ISectionBlock, IReadMoreBlock } from '../../../types/category-block';
import { getFixtures } from '../../../tests/get-fixtures';

import { fixtures } from './public-category-controller.fixtures';

const DATE_NOW = '2021-11-26T12:12:12.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Public categories controller', () => {
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

    describe('GET /categories', function () {
        it('Возвращает только неархивные рубрики', async () => {
            const { CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2, CATEGORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_1, isArchived: false });
            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_2, isArchived: false });
            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_3, isArchived: true });

            const response = await request(server).get('/categories');

            expect(response.statusCode).toBe(200);
            expect(response.body).toBeArrayOfSize(2);
            expect(response.body).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_1.urlPart }),
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_2.urlPart }),
                ])
            );
        });

        it('Может возвращать архивные и неархивные рубрики', async () => {
            const { CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2, CATEGORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_1, isArchived: false });
            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_2, isArchived: false });
            await factory.createCategory('Category', { ...CATEGORY_ATTRIBUTES_3, isArchived: true });

            const response = await request(server).get('/categories').query({ withArchived: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toBeArrayOfSize(3);
            expect(response.body).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_1.urlPart }),
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_2.urlPart }),
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_3.urlPart }),
                ])
            );
        });

        it('Возвращает рубрики по указанными тегам', async () => {
            const { CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2, CATEGORY_ATTRIBUTES_3 } = getFixtures(fixtures);
            const TAG_URL_PART_1 = 'tagUrlPart1';
            const TAG_URL_PART_2 = 'tagUrlPart2';
            const TAG_URL_PART_3 = 'tagUrlPart3';

            const tag1 = await factory.createTag('Tag', { urlPart: TAG_URL_PART_1 });
            const tag2 = await factory.createTag('Tag', { urlPart: TAG_URL_PART_2 });
            const tag3 = await factory.createTag('Tag', { urlPart: TAG_URL_PART_3 });

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES_1).then(async model => {
                await model.$add('tags', [tag1]);
                return model;
            });
            await factory.createCategory('Category', CATEGORY_ATTRIBUTES_2).then(async model => {
                await model.$add('tags', [tag2]);
                return model;
            });
            await factory.createCategory('Category', CATEGORY_ATTRIBUTES_3).then(async model => {
                await model.$add('tags', [tag1]);
                await model.$add('tags', [tag3]);
                return model;
            });

            await factory.createCategory('Category').then(async model => {
                await model.$add('tags', [tag3]);
                return model;
            });
            await factory.createCategory('Category');

            const response = await request(server)
                .get('/categories')
                .query({ tags: [tag1.urlPart, tag2.urlPart] });

            expect(response.statusCode).toBe(200);
            expect(response.body).toBeArrayOfSize(3);
            expect(response.body).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_1.urlPart }),
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_2.urlPart }),
                    expect.objectContaining({ urlPart: CATEGORY_ATTRIBUTES_3.urlPart }),
                ])
            );
        });

        it('Возвращает пустой массив, если рубрики не найдены', async () => {
            const response = await request(server).get('/categories');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual([]);
        });
    });

    describe('GET /categories/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если рубрика не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';

            const response = await request(server).get(`/categories/${NON_EXISTING_CATEGORY_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Рубрика не найдена',
                status: 404,
            });
        });

        it('Возвращает рубрику', async () => {
            const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES);

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает рубрику с тегами', async () => {
            const { CATEGORY_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const tag1 = await factory.createTag('Tag', TAG_ATTRIBUTES_1);
            const tag2 = await factory.createTag('Tag', TAG_ATTRIBUTES_2);

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает рубрику с корректным количеством постов', async () => {
            const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.publish }]);
            const categoryPosts = await factory.createManyPosts(
                'Post',
                3,
                new Array(3).fill({ status: PostStatus.publish })
            );

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('posts', categoryPosts);
                return model;
            });

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает рубрику с корректно трансформированными блоками, дополненными постами', async () => {
            const {
                CATEGORY_ATTRIBUTES,
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
            const categoryPosts = await factory.createManyPosts('Post', 10, [
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
                            two: [null, categoryPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: categoryPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    posts: [null, categoryPosts[2]?.urlPart || null, null],
                },
            };

            const CATEGORY_DATA = {
                ...CATEGORY_ATTRIBUTES,
                blocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            await factory.createCategory('Category', CATEGORY_DATA).then(async model => {
                await model.$add('posts', categoryPosts);
                return model;
            });

            const response = await request(server)
                .get(`/categories/${CATEGORY_DATA.urlPart}`)
                .query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });

    describe('GET /categories/:urlPart/draft', function () {
        it('Возвращает ошибку и статус 404, если рубрика не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';

            const response = await request(server).get(`/categories/${NON_EXISTING_CATEGORY_ID}/draft`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Рубрика не найдена',
                status: 404,
            });
        });

        it('Возвращает черновик рубрики', async () => {
            const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES);

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик рубрики с тегами', async () => {
            const { CATEGORY_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const tag1 = await factory.createTag('Tag', TAG_ATTRIBUTES_1);
            const tag2 = await factory.createTag('Tag', TAG_ATTRIBUTES_2);

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик рубрики с корректным количеством постов', async () => {
            const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 3, [{ status: PostStatus.publish }]);
            const categoryPosts = await factory.createManyPosts(
                'Post',
                3,
                new Array(3).fill({ status: PostStatus.publish })
            );

            await factory.createCategory('Category', CATEGORY_ATTRIBUTES).then(async model => {
                await model.$add('posts', categoryPosts);
                return model;
            });

            const response = await request(server).get(`/categories/${CATEGORY_ATTRIBUTES.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик рубрики с корректно трансформированными блоками, дополненными постами', async () => {
            const {
                CATEGORY_ATTRIBUTES,
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
            const categoryPosts = await factory.createManyPosts('Post', 10, [
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
                            two: [null, categoryPosts[0]?.urlPart || null],
                        },
                        {
                            type: 'oneBig',
                        },
                        {
                            type: 'oneBig',
                            oneBig: categoryPosts[1]?.urlPart,
                        },
                    ],
                },
            };
            const READ_MORE_BLOCK: IReadMoreBlock = {
                type: 'readMore',
                readMore: {
                    posts: [null, categoryPosts[2]?.urlPart || null, null],
                },
            };

            const CATEGORY_DATA = {
                ...CATEGORY_ATTRIBUTES,
                draftBlocks: [SECTION_BLOCK, READ_MORE_BLOCK],
            };

            await factory.createCategory('Category', CATEGORY_DATA).then(async model => {
                await model.$add('posts', categoryPosts);
                return model;
            });

            const response = await request(server)
                .get(`/categories/${CATEGORY_DATA.urlPart}/draft`)
                .query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });
});
