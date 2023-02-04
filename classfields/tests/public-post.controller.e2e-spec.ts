import { NestExpressApplication } from '@nestjs/platform-express';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { IPostAttributes, PostStatus } from '../../../types/post';
import { Service } from '../../../types/common';
import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { Tag as TagModel } from '../../tag/tag.model';
import { Category as CategoryModel } from '../../category/category.model';
import { Author as AuthorModel } from '../../author/author.model';
import { Post as PostModel } from '../post.model';
import { PostSchemaMarkup as PostSchemaMarkupModel } from '../../post-schema-markup/post-schema-markup.model';

import { POST_DATA_4, POST_DATA_5, POST_DATA_6, CATEGORY_DATA_1 } from './fixtures';
import { fixtures } from './public-post.controller.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Public posts controller', () => {
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

    describe('GET /posts-for-feed', function () {
        it('Возвращает опубликованные посты, отсортированные по убыванию даты создания', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 6, [
                { ...POST_ATTRIBUTES_1, createdAt: '2021-01-20T10:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:30.000Z', status: PostStatus.closed },
                { createdAt: '2021-01-20T08:10:30.000Z', status: PostStatus.draft },
                { createdAt: '2021-01-20T07:10:30.000Z', status: PostStatus.closed },
                { ...POST_ATTRIBUTES_2, createdAt: '2021-01-20T06:10:30.000Z', status: PostStatus.publish },
                { ...POST_ATTRIBUTES_3, createdAt: '2021-01-20T05:10:30.000Z', status: PostStatus.publish },
            ]);

            const response = await request(server).get('/posts-for-feed').query({ limit: 10 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает посты вместе с неархивными тегами и рубриками', async () => {
            const {
                TAG_ATTRIBUTES,
                ARCHIVED_TAG_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                ARCHIVED_CATEGORY_ATTRIBUTES,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const TAG = await TagModel.create(TAG_ATTRIBUTES);
            const ARCHIVED_TAG = await TagModel.create(ARCHIVED_TAG_ATTRIBUTES);
            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);
            const ARCHIVED_CATEGORY = await CategoryModel.create(ARCHIVED_CATEGORY_ATTRIBUTES);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);

            await PostModel.create(POST_ATTRIBUTES_2);

            await POST_1.$add('tags', [TAG, ARCHIVED_TAG]);
            await POST_1.$add('categories', [CATEGORY, ARCHIVED_CATEGORY]);

            const response = await request(server).get('/posts-for-feed');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает посты с трансформированными тегами', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES } = getFixtures(fixtures);

            const TAG = await TagModel.create(TAG_ATTRIBUTES);
            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST.$add('tag', TAG);

            const response = await request(server).get('/posts-for-feed');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает посты с авторами', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POST.$add('authors', AUTHORS);

            const response = await request(server).get('/posts-for-feed');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по "urlPart"', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);

            await PostModel.create(POST_ATTRIBUTES_3);

            const response = await request(server)
                .get('/posts-for-feed')
                .query({
                    urlPart: [POST_1.urlPart, POST_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может исключать посты с помощью "exclude"', async () => {
            await factory.createManyPosts('Post', 3, { status: PostStatus.publish });
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-03-02T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-03-02T10:00:00.000Z' });

            const response = await request(server)
                .get('/posts-for-feed')
                .query({
                    exclude: [POST_DATA_4.urlPart, POST_DATA_5.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                data: expect.toBeArrayOfSize(3),
                total: 3,
            });
            response.body.data.forEach(post => {
                expect(post).toEqual(expect.not.objectContaining({ urlPart: POST_DATA_4.urlPart }));
                expect(post).toEqual(expect.not.objectContaining({ urlPart: POST_DATA_5.urlPart }));
            });
        });

        it('Может фильтровать по тегам', async () => {
            const {
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const TAG_1 = await TagModel.create(TAG_ATTRIBUTES_1);
            const TAG_2 = await TagModel.create(TAG_ATTRIBUTES_2);
            const TAG_3 = await TagModel.create(TAG_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('tags', [TAG_1]);
            await POST_2.$add('tags', [TAG_2]);
            await POST_3.$add('tags', [TAG_1, TAG_3]);
            await POST_4.$add('tags', [TAG_3]);

            const response = await request(server)
                .get('/posts-for-feed')
                .query({
                    tags: [TAG_1.urlPart, TAG_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по категориям', async () => {
            const {
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const CATEGORY_1 = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
            const CATEGORY_2 = await CategoryModel.create(CATEGORY_ATTRIBUTES_2);
            const CATEGORY_3 = await CategoryModel.create(CATEGORY_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('categories', [CATEGORY_1]);
            await POST_2.$add('categories', [CATEGORY_2]);
            await POST_3.$add('categories', [CATEGORY_1, CATEGORY_3]);
            await POST_4.$add('categories', [CATEGORY_3]);

            const response = await request(server)
                .get('/posts-for-feed')
                .query({
                    categories: [CATEGORY_1.urlPart, CATEGORY_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        describe('Может фильтровать по статусу', () => {
            it('Возвращает опубликованные посты', async () => {
                await factory.createPost('Post', {
                    ...POST_DATA_4,
                    status: PostStatus.publish,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                await factory.createPost('Post', {
                    ...POST_DATA_5,
                    status: PostStatus.publish,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createPost('Post', {
                    status: PostStatus.publish,
                    needPublishAt: '2030-01-20T20:10:30.000Z',
                });
                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server).get('/posts-for-feed').query({
                    status: PostStatus.publish,
                    pageSize: 10,
                });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Возвращает посты с отложенной публикацией', async () => {
                const NEED_PUBLISH_AT = '2030-01-20T20:10:30.000Z';

                await factory.createPost('Post', {
                    ...POST_DATA_4,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                await factory.createPost('Post', {
                    ...POST_DATA_5,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server).get('/posts-for-feed').query({
                    status: PostStatus.delayed,
                    pageSize: 10,
                });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });
        });

        it('Может фильтровать по флагу скрытия из RSS', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createPost('Post', POST_ATTRIBUTES_1);
            await factory.createPost('Post', POST_ATTRIBUTES_2);
            await factory.createPost('Post', POST_ATTRIBUTES_3);

            const responseAll = await request(server).get('/posts-for-feed').query({
                pageSize: 10,
            });
            const responseRssOff = await request(server).get('/posts-for-feed').query({
                rssOff: 'true',
                pageSize: 10,
            });
            const responseRssOn = await request(server).get('/posts-for-feed').query({
                rssOff: 'false',
                pageSize: 10,
            });

            expect(responseAll.body).toMatchSnapshot();
            expect(responseAll.statusCode).toBe(200);

            expect(responseRssOff.body).toMatchSnapshot();
            expect(responseRssOff.statusCode).toBe(200);

            expect(responseRssOn.body).toMatchSnapshot();
            expect(responseRssOn.statusCode).toBe(200);
        });

        it('Может фильтровать по нижней дате создания', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-09T20:10:30.000Z' });

            const response = await request(server).get('/posts-for-feed').query({
                createDateFrom: '2021-01-10T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по верхней дате создания', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-22T20:10:30.000Z' });

            const response = await request(server).get('/posts-for-feed').query({
                createDateTo: '2021-01-22T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по нижней дате публикации', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-30T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-20T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-15T20:10:30.000Z',
                createdAt: '2021-01-09T20:10:30.000Z',
            });

            const response = await request(server).get('/posts-for-feed').query({
                publishDateFrom: '2021-01-16T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по верхней дате публикации', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-23T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-22T20:10:30.000Z',
            });

            const response = await request(server).get('/posts-for-feed').query({
                publishDateTo: '2021-01-25T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по МММ', async () => {
            const {
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const TAG_1 = await TagModel.create(TAG_ATTRIBUTES_1);
            const TAG_2 = await TagModel.create(TAG_ATTRIBUTES_2);
            const TAG_3 = await TagModel.create(TAG_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('tags', [TAG_1, TAG_2, TAG_3]);
            await POST_2.$add('tags', [TAG_1]);
            await POST_3.$add('tags', [TAG_2]);
            await POST_4.$add('tags', [TAG_3]);

            const response = await request(server)
                .get('/posts-for-feed')
                .query({
                    mmm: [TAG_1.mmm, TAG_2.mmm],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным лимитом', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_6, createdAt: '2021-01-09T20:10:30.000Z' });

            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-01T17:10:30.000Z' });

            const response = await request(server).get('/posts-for-feed').query({ pageSize: 3 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным лимитом и страницей', async () => {
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-18T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-19T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-20T20:10:30.000Z' });
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-17T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts-for-feed').query({ pageSize: 3, pageNumber: 1 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным оффсетом', async () => {
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-18T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-19T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-20T20:10:30.000Z' });
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-17T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts-for-feed').query({ offset: 3 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанной страницей и оффсетом', async () => {
            await factory.createManyPosts('Post', 13, [
                { createdAt: '2021-01-20T20:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T19:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T18:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T17:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T16:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T15:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T14:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T13:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T12:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T11:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T10:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T11:09:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T10:08:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:31.000Z', status: PostStatus.publish },
            ]);
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-08T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts-for-feed').query({ offset: 3, pageNumber: 1 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным оффсетом, страницей и размером страницы', async () => {
            await factory.createManyPosts('Post', 6, [
                { createdAt: '2021-01-20T20:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-19T19:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-18T18:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-17T15:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-13T16:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-12T16:10:30.000Z', status: PostStatus.publish },
            ]);
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-16T15:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-15T18:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_6, createdAt: '2021-01-14T18:10:30.000Z' });

            const response = await request(server).get('/posts-for-feed').query({
                offset: 1,
                pageNumber: 1,
                pageSize: 3,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        describe('Может сортировать посты по указанному полю', () => {
            it('Возвращает ошибку и статус 400, если указано некорректное поле', async () => {
                const response = await request(server).get('/posts-for-feed').query({ orderBySort: 'urlPart' });

                expect(response.statusCode).toBe(400);
                expect(response.body).toEqual({
                    status: 400,
                    error: 'Некорректный параметр сортировки: "urlPart"',
                });
            });

            it('Может сортировать посты по дате создания', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts-for-feed').query({ orderBySort: 'createdAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Может сортировать посты по дате обновления', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts-for-feed').query({ orderBySort: 'lastEditedAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Может сортировать посты по дате публикации', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts-for-feed').query({ orderBySort: 'publishAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });
        });

        it('Может сортировать посты в указанном направлении', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_6,
                publishAt: '2021-01-26T20:10:30.000Z',
                createdAt: '2021-01-09T20:10:30.000Z',
            });

            const response = await request(server).get('/posts-for-feed').query({
                orderBySort: 'publishAt',
                orderByAsc: true,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает data = [] и total = 0, если посты не найдены', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            const response = await request(server).get('/posts-for-feed').query({
                createDateTo: '1999-01-10T20:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [],
                total: 0,
            });
        });
    });

    describe('GET /posts', function () {
        it('Возвращает опубликованные посты, отсортированные по убыванию даты создания', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createManyPosts('Post', 6, [
                { ...POST_ATTRIBUTES_1, createdAt: '2021-01-20T10:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:30.000Z', status: PostStatus.closed },
                { createdAt: '2021-01-20T08:10:30.000Z', status: PostStatus.draft },
                { createdAt: '2021-01-20T07:10:30.000Z', status: PostStatus.closed },
                { ...POST_ATTRIBUTES_2, createdAt: '2021-01-20T06:10:30.000Z', status: PostStatus.publish },
                { ...POST_ATTRIBUTES_3, createdAt: '2021-01-20T05:10:30.000Z', status: PostStatus.publish },
            ]);

            const response = await request(server).get('/posts').query({ limit: 10 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает посты вместе с неархивными тегами и рубриками', async () => {
            const {
                TAG_ATTRIBUTES,
                ARCHIVED_TAG_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                ARCHIVED_CATEGORY_ATTRIBUTES,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const TAG = await TagModel.create(TAG_ATTRIBUTES);
            const ARCHIVED_TAG = await TagModel.create(ARCHIVED_TAG_ATTRIBUTES);
            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);
            const ARCHIVED_CATEGORY = await CategoryModel.create(ARCHIVED_CATEGORY_ATTRIBUTES);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);

            await PostModel.create(POST_ATTRIBUTES_2);

            await POST_1.$add('tags', [TAG, ARCHIVED_TAG]);
            await POST_1.$add('categories', [CATEGORY, ARCHIVED_CATEGORY]);

            const response = await request(server).get('/posts');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает посты с трансформированными тегами', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES } = getFixtures(fixtures);

            const TAG = await TagModel.create(TAG_ATTRIBUTES);
            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST.$add('tag', TAG);

            const response = await request(server).get('/posts');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по "urlPart"', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);

            await PostModel.create(POST_ATTRIBUTES_3);

            const response = await request(server)
                .get('/posts')
                .query({
                    urlPart: [POST_1.urlPart, POST_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может исключать посты с помощью "exclude"', async () => {
            await factory.createManyPosts('Post', 3, { status: PostStatus.publish });
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-03-02T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-03-02T10:00:00.000Z' });

            const response = await request(server)
                .get('/posts')
                .query({
                    exclude: [POST_DATA_4.urlPart, POST_DATA_5.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                data: expect.toBeArrayOfSize(3),
                total: 3,
            });
            response.body.data.forEach(post => {
                expect(post).toEqual(expect.not.objectContaining({ urlPart: POST_DATA_4.urlPart }));
                expect(post).toEqual(expect.not.objectContaining({ urlPart: POST_DATA_5.urlPart }));
            });
        });

        it('Может фильтровать по тегам', async () => {
            const {
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const TAG_1 = await TagModel.create(TAG_ATTRIBUTES_1);
            const TAG_2 = await TagModel.create(TAG_ATTRIBUTES_2);
            const TAG_3 = await TagModel.create(TAG_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('tags', [TAG_1]);
            await POST_2.$add('tags', [TAG_2]);
            await POST_3.$add('tags', [TAG_1, TAG_3]);
            await POST_4.$add('tags', [TAG_3]);

            const response = await request(server)
                .get('/posts')
                .query({
                    tags: [TAG_1.urlPart, TAG_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по категориям', async () => {
            const {
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const CATEGORY_1 = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
            const CATEGORY_2 = await CategoryModel.create(CATEGORY_ATTRIBUTES_2);
            const CATEGORY_3 = await CategoryModel.create(CATEGORY_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('categories', [CATEGORY_1]);
            await POST_2.$add('categories', [CATEGORY_2]);
            await POST_3.$add('categories', [CATEGORY_1, CATEGORY_3]);
            await POST_4.$add('categories', [CATEGORY_3]);

            const response = await request(server)
                .get('/posts')
                .query({
                    categories: [CATEGORY_1.urlPart, CATEGORY_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        describe('Может фильтровать по статусу', () => {
            it('Возвращает опубликованные посты', async () => {
                await factory.createPost('Post', {
                    ...POST_DATA_4,
                    status: PostStatus.publish,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                await factory.createPost('Post', {
                    ...POST_DATA_5,
                    status: PostStatus.publish,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createPost('Post', {
                    status: PostStatus.publish,
                    needPublishAt: '2030-01-20T20:10:30.000Z',
                });
                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server).get('/posts').query({
                    status: PostStatus.publish,
                    pageSize: 10,
                });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Возвращает посты с отложенной публикацией', async () => {
                const NEED_PUBLISH_AT = '2030-01-20T20:10:30.000Z';

                await factory.createPost('Post', {
                    ...POST_DATA_4,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                await factory.createPost('Post', {
                    ...POST_DATA_5,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server).get('/posts').query({
                    status: PostStatus.delayed,
                    pageSize: 10,
                });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });
        });

        it('Может фильтровать по флагу скрытия из RSS', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createPost('Post', POST_ATTRIBUTES_1);
            await factory.createPost('Post', POST_ATTRIBUTES_2);
            await factory.createPost('Post', POST_ATTRIBUTES_3);

            const responseAll = await request(server).get('/posts').query({
                pageSize: 10,
            });
            const responseRssOff = await request(server).get('/posts').query({
                rssOff: 'true',
                pageSize: 10,
            });
            const responseRssOn = await request(server).get('/posts').query({
                rssOff: 'false',
                pageSize: 10,
            });

            expect(responseAll.body).toMatchSnapshot();
            expect(responseAll.statusCode).toBe(200);

            expect(responseRssOff.body).toMatchSnapshot();
            expect(responseRssOff.statusCode).toBe(200);

            expect(responseRssOn.body).toMatchSnapshot();
            expect(responseRssOn.statusCode).toBe(200);
        });

        it('Может фильтровать по нижней дате создания', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-09T20:10:30.000Z' });

            const response = await request(server).get('/posts').query({
                createDateFrom: '2021-01-10T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по верхней дате создания', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-22T20:10:30.000Z' });

            const response = await request(server).get('/posts').query({
                createDateTo: '2021-01-22T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по нижней дате публикации', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-30T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-20T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-15T20:10:30.000Z',
                createdAt: '2021-01-09T20:10:30.000Z',
            });

            const response = await request(server).get('/posts').query({
                publishDateFrom: '2021-01-16T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по верхней дате публикации', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-23T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-22T20:10:30.000Z',
            });

            const response = await request(server).get('/posts').query({
                publishDateTo: '2021-01-25T00:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может фильтровать по МММ', async () => {
            const {
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const TAG_1 = await TagModel.create(TAG_ATTRIBUTES_1);
            const TAG_2 = await TagModel.create(TAG_ATTRIBUTES_2);
            const TAG_3 = await TagModel.create(TAG_ATTRIBUTES_3);
            const POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);
            const POST_3 = await PostModel.create(POST_ATTRIBUTES_3);
            const POST_4 = await PostModel.create(POST_ATTRIBUTES_4);

            await PostModel.create(POST_ATTRIBUTES_5);

            await POST_1.$add('tags', [TAG_1, TAG_2, TAG_3]);
            await POST_2.$add('tags', [TAG_1]);
            await POST_3.$add('tags', [TAG_2]);
            await POST_4.$add('tags', [TAG_3]);

            const response = await request(server)
                .get('/posts')
                .query({
                    mmm: [TAG_1.mmm, TAG_2.mmm],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным лимитом', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_6, createdAt: '2021-01-09T20:10:30.000Z' });

            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-01T17:10:30.000Z' });

            const response = await request(server).get('/posts').query({ pageSize: 3 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным лимитом и страницей', async () => {
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-18T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-19T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-20T20:10:30.000Z' });
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-17T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts').query({ pageSize: 3, pageNumber: 1 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным оффсетом', async () => {
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-18T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-19T20:10:30.000Z' });
            await factory.createPost('Post', { status: PostStatus.publish, createdAt: '2021-01-20T20:10:30.000Z' });
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-17T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts').query({ offset: 3 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанной страницей и оффсетом', async () => {
            await factory.createManyPosts('Post', 13, [
                { createdAt: '2021-01-20T20:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T19:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T18:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T17:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T16:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T15:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T14:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T13:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T12:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T11:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T10:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T11:09:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T10:08:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-20T09:10:31.000Z', status: PostStatus.publish },
            ]);
            const POST_DATA = { ...POST_DATA_4, createdAt: '2021-01-08T20:10:30.000Z' };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/posts').query({ offset: 3, pageNumber: 1 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может отдавать посты с указанным оффсетом, страницей и размером страницы', async () => {
            await factory.createManyPosts('Post', 6, [
                { createdAt: '2021-01-20T20:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-19T19:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-18T18:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-17T15:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-13T16:10:30.000Z', status: PostStatus.publish },
                { createdAt: '2021-01-12T16:10:30.000Z', status: PostStatus.publish },
            ]);
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-16T15:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-15T18:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_6, createdAt: '2021-01-14T18:10:30.000Z' });

            const response = await request(server).get('/posts').query({
                offset: 1,
                pageNumber: 1,
                pageSize: 3,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может сортировать посты по указанному полю', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                publishAt: '2021-01-26T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_5,
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-21T20:10:30.000Z',
            });
            await factory.createPost('Post', {
                ...POST_DATA_6,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-22T20:10:30.000Z',
            });

            const response = await request(server).get('/posts').query({ orderBySort: 'publishAt' });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        describe('Может сортировать посты по указанному полю', () => {
            it('Возвращает ошибку и статус 400, если указано некорректное поле', async () => {
                const response = await request(server).get('/posts').query({ orderBySort: 'urlPart' });

                expect(response.statusCode).toBe(400);
                expect(response.body).toEqual({
                    status: 400,
                    error: 'Некорректный параметр сортировки: "urlPart"',
                });
            });

            it('Может сортировать посты по дате создания', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts').query({ orderBySort: 'createdAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Может сортировать посты по дате обновления', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts').query({ orderBySort: 'lastEditedAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });

            it('Может сортировать посты по дате публикации', async () => {
                const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

                await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

                const response = await request(server).get('/posts').query({ orderBySort: 'publishAt' });

                expect(response.statusCode).toBe(200);
                expect(response.body).toMatchSnapshot();
            });
        });

        it('Возвращает data = [] и total = 0, если посты не найдены', async () => {
            await factory.createPost('Post', { ...POST_DATA_4, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_5, createdAt: '2021-01-10T20:10:30.000Z' });

            const response = await request(server).get('/posts').query({
                createDateTo: '1999-01-10T20:10:30.000Z',
                pageSize: 10,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [],
                total: 0,
            });
        });
    });

    describe('GET /posts/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).get(`/posts/${NON_EXISTING_POST_ID}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает пост со всеми тегами и неархивными рубриками', async () => {
            const {
                ARCHIVED_TAG_ATTRIBUTES,
                TAG_ATTRIBUTES,
                ARCHIVED_CATEGORY_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                POST_ATTRIBUTES,
            } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES, ARCHIVED_TAG_ATTRIBUTES]);
            const CATEGORIES = await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES, ARCHIVED_CATEGORY_ATTRIBUTES]);

            await POST.$add('tags', TAGS);
            await POST.$add('categories', CATEGORIES);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с выбранной предыдущей статьей', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

            const BEFORE_POST = await PostModel.create(POST_ATTRIBUTES_1);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES_2, before: BEFORE_POST.urlPart });

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с предыдущей статьей по категории, если выбранная статья не опубликована', async () => {
            const { POST_ATTRIBUTES, BEFORE_POST_ATTRIBUTES, CATEGORY_POST_ATTRIBUTES, CATEGORY_ATTRIBUTES } =
                getFixtures(fixtures);

            const CATEGORY_POST = await PostModel.create(CATEGORY_POST_ATTRIBUTES);
            const BEFORE_POST = await PostModel.create(BEFORE_POST_ATTRIBUTES);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES, before: BEFORE_POST.urlPart });
            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

            await CATEGORY_POST.$add('category', CATEGORY);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с пустой предыдущей статьей', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с выбранной следующей статьей', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

            const AFTER_POST = await PostModel.create(POST_ATTRIBUTES_1);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES_2, before: AFTER_POST.urlPart });

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост со следующей статьей по категории, если выбранная статья не опубликована', async () => {
            const { POST_ATTRIBUTES, AFTER_POST_ATTRIBUTES, CATEGORY_POST_ATTRIBUTES, CATEGORY_ATTRIBUTES } =
                getFixtures(fixtures);

            const CATEGORY_POST = await PostModel.create(CATEGORY_POST_ATTRIBUTES);
            const AFTER_POST = await PostModel.create(AFTER_POST_ATTRIBUTES);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES, after: AFTER_POST.urlPart });

            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

            await CATEGORY_POST.$add('category', CATEGORY);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с пустой следующей статьей', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с моделью опроса в блоке опроса', async () => {
            const poll = await factory.createPoll('Poll', {
                question: 'Что бы вы выбрали?',
                answers: ['BMW', 'LADA', 'Honda'],
            });

            await factory.createPost('Post', {
                ...POST_DATA_4,
                blocks: [
                    {
                        type: 'text',
                        text: 'Интересная статья',
                    },
                    {
                        type: 'poll',
                        poll: {
                            id: poll.id,
                        },
                    },
                ],
            });

            const response = await request(server).get(`/posts/${POST_DATA_4.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост без блока опроса, если опрос не найден', async () => {
            await factory.createPost('Post', {
                ...POST_DATA_4,
                blocks: [
                    {
                        type: 'text',
                        text: 'Интересная статья',
                    },
                    {
                        type: 'poll',
                        poll: {
                            id: 1000,
                        },
                    },
                ],
            });

            const response = await request(server).get(`/posts/${POST_DATA_4.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может возвращать пост с моделью поста в блоке "материал по теме"', async () => {
            const post1 = await factory.createPost('Post', POST_DATA_4);

            await factory.createPost('Post', {
                ...POST_DATA_5,
                blocks: [
                    {
                        type: 'post',
                        post: {
                            title: 'Интересный пост',
                            urlPart: post1.urlPart,
                        },
                    },
                ],
            });

            const response = await request(server)
                .get(`/posts/${POST_DATA_5.urlPart}`)
                .query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с авторами', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POST.$add('authors', AUTHORS);

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с микроразметкой FAQPage', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await PostSchemaMarkupModel.create({ postId: POST.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES });

            const response = await request(server).get(`/posts/${POST.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает ошибку и статус 404 при запросе неопубликованного поста', async () => {
            const draftPost = await factory.createPost('Post', { status: PostStatus.draft });

            const response = await request(server).get(`/posts/${draftPost.urlPart}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });
    });

    describe('GET /posts/:urlPart/draft', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).get(`/posts/${NON_EXISTING_POST_ID}/draft`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает черновик поста со всеми тегами и неархивными рубриками', async () => {
            const {
                ARCHIVED_TAG_ATTRIBUTES,
                TAG_ATTRIBUTES,
                ARCHIVED_CATEGORY_ATTRIBUTES,
                CATEGORY_ATTRIBUTES,
                POST_ATTRIBUTES,
            } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES, ARCHIVED_TAG_ATTRIBUTES]);
            const CATEGORIES = await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES, ARCHIVED_CATEGORY_ATTRIBUTES]);

            await POST.$add('tags', TAGS);
            await POST.$add('categories', CATEGORIES);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста с выбранной предыдущей статьей', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

            const BEFORE_POST = await PostModel.create(POST_ATTRIBUTES_1);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES_2, before: BEFORE_POST.urlPart });

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста с предыдущей статьей по категории, если выбранная статья не опубликована', async () => {
            const { POST_ATTRIBUTES, BEFORE_POST_ATTRIBUTES, CATEGORY_POST_ATTRIBUTES, CATEGORY_ATTRIBUTES } =
                getFixtures(fixtures);

            const CATEGORY_POST = await PostModel.create(CATEGORY_POST_ATTRIBUTES);
            const BEFORE_POST = await PostModel.create(BEFORE_POST_ATTRIBUTES);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES, before: BEFORE_POST.urlPart });
            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

            await CATEGORY_POST.$add('category', CATEGORY);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста с пустой предыдущей статьей', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с выбранной следующей статьей', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

            const AFTER_POST = await PostModel.create(POST_ATTRIBUTES_1);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES_2, before: AFTER_POST.urlPart });

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста со следующей статьей по категории, если выбранная статья не опубликована', async () => {
            const { POST_ATTRIBUTES, AFTER_POST_ATTRIBUTES, CATEGORY_POST_ATTRIBUTES, CATEGORY_ATTRIBUTES } =
                getFixtures(fixtures);

            const CATEGORY_POST = await PostModel.create(CATEGORY_POST_ATTRIBUTES);
            const AFTER_POST = await PostModel.create(AFTER_POST_ATTRIBUTES);
            const POST = await PostModel.create({ ...POST_ATTRIBUTES, after: AFTER_POST.urlPart });

            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

            await CATEGORY_POST.$add('category', CATEGORY);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста с пустой следующей статьей', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста с моделью опроса в блоке опроса', async () => {
            const poll = await factory.createPoll('Poll', {
                question: 'Что бы вы выбрали?',
                answers: ['BMW', 'LADA', 'Honda'],
            });
            const POST_DATA: Partial<IPostAttributes> = {
                ...POST_DATA_4,
                draftBlocks: [
                    {
                        type: 'text',
                        text: 'Интересная статья',
                    },
                    {
                        type: 'poll',
                        poll: {
                            id: poll.id,
                        },
                    },
                ],
            };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/posts/${POST_DATA.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает черновик поста без блока опроса, если опрос не найден', async () => {
            const POST_DATA: Partial<IPostAttributes> = {
                ...POST_DATA_4,
                draftBlocks: [
                    {
                        type: 'text',
                        text: 'Интересная статья',
                    },
                    {
                        type: 'poll',
                        poll: {
                            id: 1000,
                        },
                    },
                ],
            };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/posts/${POST_DATA.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Может возвращать черновик поста с моделью поста в блоке "материал по теме"', async () => {
            const blockPost = await factory.createPost('Post', POST_DATA_4);
            const POST_DATA: Partial<IPostAttributes> = {
                ...POST_DATA_5,
                draftBlocks: [
                    {
                        type: 'post',
                        post: {
                            title: 'Интересный пост',
                            urlPart: blockPost.urlPart,
                        },
                    },
                ],
            };

            await factory.createPost('Post', POST_DATA);

            const response = await request(server)
                .get(`/posts/${POST_DATA.urlPart}/draft`)
                .query({ withPostsModels: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с авторами', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POST.$add('authors', AUTHORS);

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает пост с микроразметкой FAQPage', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await PostSchemaMarkupModel.create({ postId: POST.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES });

            const response = await request(server).get(`/posts/${POST.urlPart}/draft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });

    describe('GET /posts/realty/legacyMapper', () => {
        const LEGACY_TAG = 'вторичное жильё';
        const NEW_CATEGORY = 'vtorichnoe-zhilyo';
        let category;

        beforeEach(async () => {
            await factory.createManyPosts('Post', 2, [
                { service: Service.realty, status: PostStatus.draft },
                { service: Service.autoru, status: PostStatus.publish },
            ]);
            category = await factory.createCategory('Category', { ...CATEGORY_DATA_1, urlPart: NEW_CATEGORY });
        });

        it('Возвращает опубликованные посты недвижимости, отсортированные по возрастанию даты создания', async () => {
            await factory.createPost('Post', POST_DATA_4);
            await factory.createPost('Post', POST_DATA_5);

            const response = await request(server).get('/posts/realty/legacyMapper');

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает посты для указанного легаси-тега', async () => {
            const post1 = await factory.createPost('Post', POST_DATA_4);
            const post2 = await factory.createPost('Post', POST_DATA_5);

            await category.addPosts([post1, post2]);

            const response = await request(server).get('/posts/realty/legacyMapper').query({ tag: LEGACY_TAG });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает указанное количество постов', async () => {
            const post1 = await factory.createPost('Post', POST_DATA_4);
            const post2 = await factory.createPost('Post', POST_DATA_5);
            const post3 = await factory.createPost('Post', POST_DATA_6);

            await category.addPosts([post1, post2, post3]);

            const response = await request(server)
                .get('/posts/realty/legacyMapper')
                .query({ tag: LEGACY_TAG, size: 2 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает ошибку и статус 500, если не удалось смапить легаси-тег на категорию', async () => {
            const UNKNOWN_TAG = 'unknown-tag';

            await factory.createPost('Post', POST_DATA_4);
            await factory.createPost('Post', POST_DATA_5);
            await factory.createPost('Post', POST_DATA_6);

            const response = await request(server).get('/posts/realty/legacyMapper').query({
                tag: UNKNOWN_TAG,
                size: 10,
            });

            expect(response.body).toEqual({
                status: 500,
                error: 'Нет такой категории!',
            });
            expect(response.statusCode).toBe(500);
        });
    });

    describe('GET /categories/:urlPart/morePosts', function () {
        it('Возвращает ошибку и статус 404, если категория не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';

            const response = await request(server).get(`/categories/${NON_EXISTING_CATEGORY_ID}/morePosts`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Рубрика не найдена',
            });
            expect(response.statusCode).toBe(404);
        });
    });

    describe('GET /tags/:urlPart/morePosts', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag-id';

            const response = await request(server).get(`/tags/${NON_EXISTING_TAG_ID}/morePosts`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
            expect(response.statusCode).toBe(404);
        });
    });

    describe('GET /postsForSection', function () {
        it('Возвращает пустой набор постов и статус 200, если посты не найдены', async () => {
            const response = await request(server).get('/postsForSection');

            expect(response.body).toEqual({
                total: 0,
                data: [],
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает посты с указанной категорией и тегом', async () => {
            const {
                DRAFT_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_2,
                PUBLISH_POST_ATTRIBUTES_3,
                PUBLISH_POST_ATTRIBUTES_4,
                PUBLISH_POST_ATTRIBUTES_5,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const [PUBLISH_POST_1, PUBLISH_POST_2, PUBLISH_POST_3, PUBLISH_POST_4, DRAFT_POST] =
                await PostModel.bulkCreate([
                    PUBLISH_POST_ATTRIBUTES_1,
                    PUBLISH_POST_ATTRIBUTES_2,
                    PUBLISH_POST_ATTRIBUTES_3,
                    PUBLISH_POST_ATTRIBUTES_4,
                    PUBLISH_POST_ATTRIBUTES_5,
                    DRAFT_POST_ATTRIBUTES_1,
                ]);

            const SEARCH_TAG = await TagModel.create(TAG_ATTRIBUTES_1);
            const OTHER_TAG = await TagModel.create(TAG_ATTRIBUTES_2);
            const SEARCH_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
            const OTHER_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_2);

            await PUBLISH_POST_1?.$add('tags', [SEARCH_TAG]);
            await PUBLISH_POST_1?.$add('categories', [SEARCH_CATEGORY]);

            await PUBLISH_POST_2?.$add('tags', [SEARCH_TAG]);
            await PUBLISH_POST_2?.$add('categories', [SEARCH_CATEGORY, OTHER_CATEGORY]);

            await PUBLISH_POST_3?.$add('tags', [SEARCH_TAG, OTHER_TAG]);
            await PUBLISH_POST_3?.$add('categories', [SEARCH_CATEGORY, OTHER_CATEGORY]);

            await PUBLISH_POST_4?.$add('tags', [OTHER_TAG]);
            await PUBLISH_POST_4?.$add('categories', [SEARCH_CATEGORY]);

            await DRAFT_POST?.$add('tags', [SEARCH_TAG]);
            await DRAFT_POST?.$add('categories', [OTHER_CATEGORY]);

            const response = await request(server).get('/postsForSection').query({
                tags: SEARCH_TAG.urlPart,
                categories: SEARCH_CATEGORY.urlPart,
                pageSize: 10,
                pageNumber: 0,
            });

            expect(response.body).toMatchSnapshot();
        });

        it('Возвращает посты с указанными категориями и тегами', async () => {
            const {
                DRAFT_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_2,
                PUBLISH_POST_ATTRIBUTES_3,
                PUBLISH_POST_ATTRIBUTES_4,
                PUBLISH_POST_ATTRIBUTES_5,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
                PUBLISH_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_2,
                PUBLISH_POST_ATTRIBUTES_3,
                PUBLISH_POST_ATTRIBUTES_4,
                PUBLISH_POST_ATTRIBUTES_5,
                DRAFT_POST_ATTRIBUTES_1,
            ]);

            const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            ]);

            const [CATEGORY_1, CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('categories', [CATEGORY_1 as CategoryModel]);

            await POST_2?.reload();
            await POST_2?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel]);
            await POST_2?.$add('categories', [CATEGORY_3 as CategoryModel, CATEGORY_2 as CategoryModel]);

            await POST_3?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel]);
            await POST_3?.$add('categories', [CATEGORY_1 as CategoryModel, CATEGORY_2 as CategoryModel]);

            await POST_4?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel, TAG_3 as TagModel]);
            await POST_4?.$add('categories', [CATEGORY_2 as CategoryModel, CATEGORY_3 as CategoryModel]);

            await POST_5?.$add('categories', [
                CATEGORY_1 as CategoryModel,
                CATEGORY_2 as CategoryModel,
                CATEGORY_3 as CategoryModel,
            ]);
            await POST_5?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel, TAG_3 as TagModel]);

            const response = await request(server)
                .get('/postsForSection')
                .query({
                    tags: [TAG_1?.urlPart, TAG_2?.urlPart],
                    categories: [CATEGORY_2?.urlPart, CATEGORY_3?.urlPart],
                    pageSize: 2,
                    pageNumber: 1,
                });

            expect(response.body).toEqual({
                total: 3,
                data: [
                    {
                        id: POST_2?.id,
                        urlPart: 'exeed-vx-zametili-v-moskve-pered-reklamnoy-syomkoy',
                        title: 'Флагманский китайский кроссовер Exeed VX заметили на парковке в Москве',
                        lead: 'Один из первых товарных экземпляров флагманского кроссовера Exeed VX заметили в Москве во время подготовки к рекламной съёмке',
                        publishAt: '2021-09-10T10:00:00.000Z',
                        tags: [
                            {
                                urlPart: 'news',
                                title: 'Новости',
                                isHot: false,
                                isPartnership: false,
                                mmm: 'test-mmm2',
                            },
                            {
                                urlPart: 'podcast',
                                title: 'Подкаст',
                                isHot: false,
                                isPartnership: false,
                                mmm: 'test-mmm1',
                            },
                        ],
                        categories: [
                            {
                                title: '1 серия',
                                urlPart: 'bmw-1er',
                            },
                        ],
                    },
                ],
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает посты с авторами', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } =
                getFixtures(fixtures);

            const POSTS = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2]);
            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POSTS[0]?.$add('authors', AUTHORS[0] as AuthorModel);
            await POSTS[1]?.$add('authors', AUTHORS[1] as AuthorModel);

            const response = await request(server).get(`/postsForSection`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchSnapshot();
        });
    });
});
