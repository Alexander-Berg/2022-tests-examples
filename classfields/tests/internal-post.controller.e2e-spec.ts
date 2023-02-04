import { NestExpressApplication } from '@nestjs/platform-express';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { PostStatus } from '../../../types/post';
import { Service } from '../../../types/common';
import { Post as PostModel } from '../post.model';
import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { Author as AuthorModel } from '../../author/author.model';
import { PostSchemaMarkup as PostSchemaMarkupModel } from '../../post-schema-markup/post-schema-markup.model';

import {
    COMMIT_DRAFT_POST_DATA_1,
    CREATE_POST_DATA_1,
    POST_DATA_1,
    POST_DATA_2,
    POST_DATA_3,
    PUBLISH_POST_DATA_1,
    UNPUBLISH_POST_DATA_1,
    UPDATE_POST_DATA_1,
} from './fixtures';
import { fixtures } from './internal-post.controller.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Internal posts controller', () => {
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

    describe('GET /internal/posts', function () {
        const DEFAULT_LIMIT = 10;

        it('Возвращает 10 постов, отсортированных по убыванию даты создания', async () => {
            const posts = await factory
                .createManyPosts('Post', 12, [
                    { createdAt: '2021-01-20T20:10:30.000Z', status: PostStatus.publish },
                    { createdAt: '2021-01-20T19:10:30.000Z', status: PostStatus.closed },
                    { createdAt: '2021-01-20T18:10:30.000Z', status: PostStatus.draft },
                    { createdAt: '2021-01-20T17:10:30.000Z', status: PostStatus.closed },
                    { createdAt: '2021-01-20T16:10:30.000Z' },
                    { createdAt: '2021-01-20T15:10:30.000Z' },
                    { createdAt: '2021-01-20T14:10:30.000Z' },
                    { createdAt: '2021-01-20T13:10:30.000Z' },
                    { createdAt: '2021-01-20T12:10:30.000Z' },
                    { createdAt: '2021-01-20T11:10:30.000Z' },
                    { createdAt: '2021-01-20T10:10:30.000Z' },
                    { createdAt: '2021-01-20T09:10:30.000Z' },
                ])
                .then(models =>
                    models.map(model => ({
                        urlPart: model.urlPart,
                        id: model.id,
                        service: model.service,
                        title: model.title,
                        titleRss: model.titleRss,
                        titleApp: model.titleApp,
                        draftTitle: model.draftTitle,
                        draftTitleRss: model.draftTitleRss,
                        draftTitleApp: model.draftTitleApp,
                        status: model.status,
                        lead: model.lead,
                        draftBlocks: model.draftBlocks,
                        draftMainImage: model.draftMainImage,
                        author: model.author,
                        commentsOff: false,
                        rssOff: false,
                        advertisementOff: false,
                        subscribeOff: false,
                        indexOff: false,
                        shouldHaveFaqPage: false,
                        tags: [],
                        categories: [],
                        createdAt: model.createdAt && new Date(model.createdAt).toISOString(),
                        lastEditedAt: model.lastEditedAt && new Date(model.lastEditedAt).toISOString(),
                    }))
                );

            const response = await request(server).get('/internal/posts');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: posts.slice(0, DEFAULT_LIMIT),
                total: 12,
            });
        });

        it('Возвращает посты со всеми тегами и рубриками', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false, urlPart: 'arenda' });
            const tag2 = await factory.createTag('Tag', { isArchived: true, urlPart: 'ipoteka' });
            const category1 = await factory.createCategory('Category', { isArchived: false, urlPart: 'arenda' });
            const category2 = await factory.createCategory('Category', { isArchived: true, urlPart: 'ipoteka' });
            const post = await factory.createPost('Post', POST_DATA_1).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                await model.$add('categories', [category1, category2]);
                return model;
            });

            const response = await request(server).get('/internal/posts');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        tags: [
                            {
                                urlPart: tag1.urlPart,
                                service: tag1.service,
                                title: tag1.title,
                                shortTitle: tag1.shortTitle,
                                blocks: tag1.blocks,
                                draftTitle: tag1.draftTitle,
                                draftShortTitle: tag1.draftShortTitle,
                                draftBlocks: tag1.draftBlocks,
                                isArchived: tag1.isArchived,
                                isHot: tag1.isHot,
                                isPartnership: tag1.isPartnership,
                                partnershipLink: tag1.partnershipLink,
                                partnershipName: tag1.partnershipName,
                                partnershipBadgeName: tag1.partnershipBadgeName,
                                metaTitle: tag1.metaTitle,
                                metaDescription: tag1.metaDescription,
                                mmm: tag1.mmm,
                                createdAt: new Date(tag1.createdAt).toISOString(),
                                lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                            },
                            {
                                urlPart: tag2.urlPart,
                                service: tag2.service,
                                title: tag2.title,
                                shortTitle: tag2.shortTitle,
                                blocks: tag2.blocks,
                                draftTitle: tag2.draftTitle,
                                draftShortTitle: tag2.draftShortTitle,
                                draftBlocks: tag2.draftBlocks,
                                isArchived: tag2.isArchived,
                                isHot: tag2.isHot,
                                isPartnership: tag2.isPartnership,
                                partnershipLink: tag2.partnershipLink,
                                partnershipName: tag2.partnershipName,
                                partnershipBadgeName: tag2.partnershipBadgeName,
                                metaTitle: tag2.metaTitle,
                                metaDescription: tag2.metaDescription,
                                mmm: tag2.mmm,
                                createdAt: new Date(tag2.createdAt).toISOString(),
                                lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                            },
                        ],
                        categories: [
                            {
                                ...category1.toJSON(),
                                createdAt: new Date(category1.createdAt).toISOString(),
                                lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                            },
                            {
                                ...category2.toJSON(),
                                createdAt: new Date(category2.createdAt).toISOString(),
                                lastEditedAt: new Date(category2.lastEditedAt).toISOString(),
                            },
                        ],
                        createdAt: post.createdAt && post.createdAt && new Date(post.createdAt).toISOString(),
                        lastEditedAt:
                            post.lastEditedAt && post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
                    },
                ],
                total: 1,
            });
        });

        it('Возвращает посты без архивных тегов и рубрик', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false });
            const tag2 = await factory.createTag('Tag', { isArchived: true });
            const category1 = await factory.createCategory('Category', { isArchived: false });
            const category2 = await factory.createCategory('Category', { isArchived: true });
            const post1 = await factory.createPost('Post', POST_DATA_1).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                await model.$add('categories', [category1, category2]);
                return model;
            });
            const post2 = await factory.createPost('Post', POST_DATA_2);

            const response = await request(server).get('/internal/posts').query({ withArchived: 'false' });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        tags: [
                            {
                                urlPart: tag1.urlPart,
                                service: tag1.service,
                                title: tag1.title,
                                shortTitle: tag1.shortTitle,
                                blocks: tag1.blocks,
                                draftTitle: tag1.draftTitle,
                                draftShortTitle: tag1.draftShortTitle,
                                draftBlocks: tag1.draftBlocks,
                                isArchived: tag1.isArchived,
                                isHot: tag1.isHot,
                                isPartnership: tag1.isPartnership,
                                partnershipLink: tag1.partnershipLink,
                                partnershipName: tag1.partnershipName,
                                partnershipBadgeName: tag1.partnershipBadgeName,
                                metaTitle: tag1.metaTitle,
                                metaDescription: tag1.metaDescription,
                                mmm: tag1.mmm,
                                createdAt: new Date(tag1.createdAt).toISOString(),
                                lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                            },
                        ],
                        categories: [
                            {
                                ...category1.toJSON(),
                                createdAt: new Date(category1.createdAt).toISOString(),
                                lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                            },
                        ],
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_2,
                        tags: [],
                        categories: [],
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                    },
                ],
                total: 2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по "urlPart"', async () => {
            await factory.createManyPosts('Post', 5);
            const post1 = await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            const post2 = await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T10:00:00.000Z' });

            const response = await request(server)
                .get('/internal/posts')
                .query({
                    urlPart: [POST_DATA_1.urlPart, POST_DATA_2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        tags: [],
                        categories: [],
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_2,
                        tags: [],
                        categories: [],
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по тегам', async () => {
            const tag1 = await factory.createTag('Tag', { urlPart: 'arenda' });
            const tag2 = await factory.createTag('Tag', { urlPart: 'ipoteka' });
            const tag3 = await factory.createTag('Tag', { urlPart: 'vtorichka' });

            const post1 = await factory
                .createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' })
                .then(async model => {
                    await model.$add('tag', tag1);
                    return model;
                });
            const post2 = await factory
                .createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T10:00:00.000Z' })
                .then(async model => {
                    await model.$add('tag', tag2);
                    return model;
                });
            const post3 = await factory
                .createPost('Post', { ...POST_DATA_3, createdAt: '2021-01-09T10:00:00.000Z' })
                .then(async model => {
                    await model.$add('tag', tag1);
                    await model.$add('tag', tag3);
                    return model;
                });

            await factory.createPost('Post').then(async model => {
                await model.$add('tag', tag3);
                return model;
            });
            await factory.createPost('Post');

            const response = await request(server)
                .get('/internal/posts')
                .query({
                    tags: [tag1.urlPart, tag2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        tags: [
                            {
                                urlPart: tag1.urlPart,
                                service: tag1.service,
                                title: tag1.title,
                                shortTitle: tag1.shortTitle,
                                blocks: tag1.blocks,
                                draftTitle: tag1.draftTitle,
                                draftShortTitle: tag1.draftShortTitle,
                                draftBlocks: tag1.draftBlocks,
                                isArchived: tag1.isArchived,
                                isHot: tag1.isHot,
                                isPartnership: tag1.isPartnership,
                                partnershipLink: tag1.partnershipLink,
                                partnershipName: tag1.partnershipName,
                                partnershipBadgeName: tag1.partnershipBadgeName,
                                metaTitle: tag1.metaTitle,
                                metaDescription: tag1.metaDescription,
                                mmm: tag1.mmm,
                                createdAt: new Date(tag1.createdAt).toISOString(),
                                lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                            },
                        ],
                        categories: [],
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_2,
                        tags: [
                            {
                                urlPart: tag2.urlPart,
                                service: tag2.service,
                                title: tag2.title,
                                shortTitle: tag2.shortTitle,
                                blocks: tag2.blocks,
                                draftTitle: tag2.draftTitle,
                                draftShortTitle: tag2.draftShortTitle,
                                draftBlocks: tag2.draftBlocks,
                                isArchived: tag2.isArchived,
                                isHot: tag2.isHot,
                                isPartnership: tag2.isPartnership,
                                partnershipLink: tag2.partnershipLink,
                                partnershipName: tag2.partnershipName,
                                partnershipBadgeName: tag2.partnershipBadgeName,
                                metaTitle: tag2.metaTitle,
                                metaDescription: tag2.metaDescription,
                                mmm: tag2.mmm,
                                createdAt: new Date(tag2.createdAt).toISOString(),
                                lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                            },
                        ],
                        categories: [],
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_3,
                        tags: [
                            {
                                urlPart: tag1.urlPart,
                                service: tag1.service,
                                title: tag1.title,
                                shortTitle: tag1.shortTitle,
                                blocks: tag1.blocks,
                                draftTitle: tag1.draftTitle,
                                draftShortTitle: tag1.draftShortTitle,
                                draftBlocks: tag1.draftBlocks,
                                isArchived: tag1.isArchived,
                                isHot: tag1.isHot,
                                isPartnership: tag1.isPartnership,
                                partnershipLink: tag1.partnershipLink,
                                partnershipName: tag1.partnershipName,
                                partnershipBadgeName: tag1.partnershipBadgeName,
                                metaTitle: tag1.metaTitle,
                                metaDescription: tag1.metaDescription,
                                mmm: tag1.mmm,
                                createdAt: new Date(tag1.createdAt).toISOString(),
                                lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                            },
                            {
                                urlPart: tag3.urlPart,
                                service: tag3.service,
                                title: tag3.title,
                                shortTitle: tag3.shortTitle,
                                blocks: tag3.blocks,
                                draftTitle: tag3.draftTitle,
                                draftShortTitle: tag3.draftShortTitle,
                                draftBlocks: tag3.draftBlocks,
                                isArchived: tag3.isArchived,
                                isHot: tag3.isHot,
                                isPartnership: tag3.isPartnership,
                                partnershipLink: tag3.partnershipLink,
                                partnershipName: tag3.partnershipName,
                                partnershipBadgeName: tag3.partnershipBadgeName,
                                metaTitle: tag3.metaTitle,
                                metaDescription: tag3.metaDescription,
                                mmm: tag3.mmm,
                                createdAt: new Date(tag3.createdAt).toISOString(),
                                lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                            },
                        ],
                        categories: [],
                        createdAt: post3.createdAt && new Date(post3.createdAt).toISOString(),
                        lastEditedAt:
                            post3.lastEditedAt && post3.lastEditedAt && new Date(post3.lastEditedAt).toISOString(),
                    },
                ],
                total: 3,
            });
        });

        it('Может фильтровать по категориям', async () => {
            const category1 = await factory.createCategory('Category', { urlPart: 'arenda' });
            const category2 = await factory.createCategory('Category', { urlPart: 'ipoteka' });
            const category3 = await factory.createCategory('Category', { urlPart: 'vtorichka' });

            const post1 = await factory
                .createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' })
                .then(async model => {
                    await model.$add('category', category1);
                    return model;
                });
            const post2 = await factory
                .createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T10:00:00.000Z' })
                .then(async model => {
                    await model.$add('category', category2);
                    return model;
                });
            const post3 = await factory
                .createPost('Post', { ...POST_DATA_3, createdAt: '2021-01-09T10:00:00.000Z' })
                .then(async model => {
                    await model.$add('category', category1);
                    await model.$add('category', category3);
                    return model;
                });

            await factory.createPost('Post').then(async model => {
                await model.$add('category', category3);
                return model;
            });
            await factory.createPost('Post');

            const response = await request(server)
                .get('/internal/posts')
                .query({
                    categories: [category1.urlPart, category2.urlPart],
                    pageSize: 10,
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        categories: [
                            {
                                ...category1.toJSON(),
                                createdAt: new Date(category1.createdAt).toISOString(),
                                lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                            },
                        ],
                        tags: [],
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_2,
                        categories: [
                            {
                                ...category2.toJSON(),
                                createdAt: new Date(category2.createdAt).toISOString(),
                                lastEditedAt: new Date(category2.lastEditedAt).toISOString(),
                            },
                        ],
                        tags: [],
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_3,
                        categories: [
                            {
                                ...category1.toJSON(),
                                createdAt: new Date(category1.createdAt).toISOString(),
                                lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                            },
                            {
                                ...category3.toJSON(),
                                createdAt: new Date(category3.createdAt).toISOString(),
                                lastEditedAt: new Date(category3.lastEditedAt).toISOString(),
                            },
                        ],
                        tags: [],
                        createdAt: post3.createdAt && new Date(post3.createdAt).toISOString(),
                        lastEditedAt: post3.lastEditedAt && new Date(post3.lastEditedAt).toISOString(),
                    },
                ],
                total: 3,
            });
        });

        it('Может фильтровать по сервису', async () => {
            const post1 = await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            const post2 = await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { service: Service.autoru });

            const response = await request(server).get('/internal/posts').query({ service: 'realty', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
        });

        describe('Может фильтровать по статусу', () => {
            it('Возвращает посты снятые с публикации', async () => {
                const post1 = await factory.createPost('Post', {
                    ...POST_DATA_1,
                    status: PostStatus.closed,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                const post2 = await factory.createPost('Post', {
                    ...POST_DATA_2,
                    status: PostStatus.closed,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createManyPosts('Post', 3, { status: PostStatus.publish });

                const response = await request(server)
                    .get('/internal/posts')
                    .query({ status: PostStatus.closed, pageSize: 10 });

                expect(response.statusCode).toBe(200);
                expect(response.body).toEqual({
                    data: [
                        {
                            ...POST_DATA_1,
                            id: post1.id,
                            status: PostStatus.closed,
                            createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                            lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                        {
                            ...POST_DATA_2,
                            id: post2.id,
                            status: PostStatus.closed,
                            createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                            lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                    ],
                    total: 2,
                });
            });

            it('Возвращает черновые посты', async () => {
                const post1 = await factory.createPost('Post', {
                    ...POST_DATA_1,
                    status: PostStatus.draft,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                const post2 = await factory.createPost('Post', {
                    ...POST_DATA_2,
                    status: PostStatus.draft,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createManyPosts('Post', 3, { status: PostStatus.publish });

                const response = await request(server)
                    .get('/internal/posts')
                    .query({ status: PostStatus.draft, pageSize: 10 });

                expect(response.statusCode).toBe(200);
                expect(response.body).toEqual({
                    data: [
                        {
                            ...POST_DATA_1,
                            id: post1.id,
                            status: PostStatus.draft,
                            createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                            lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                        {
                            ...POST_DATA_2,
                            id: post2.id,
                            status: PostStatus.draft,
                            createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                            lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                    ],
                    total: 2,
                });
            });

            it('Возвращает опубликованные посты', async () => {
                const post1 = await factory.createPost('Post', {
                    ...POST_DATA_1,
                    status: PostStatus.publish,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                const post2 = await factory.createPost('Post', {
                    ...POST_DATA_2,
                    status: PostStatus.publish,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createPost('Post', {
                    status: PostStatus.publish,
                    needPublishAt: '2030-01-20T20:10:30.000Z',
                });
                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server)
                    .get('/internal/posts')
                    .query({ status: PostStatus.publish, pageSize: 10 });

                expect(response.statusCode).toBe(200);
                expect(response.body).toEqual({
                    data: [
                        {
                            ...POST_DATA_1,
                            id: post1.id,
                            status: PostStatus.publish,
                            createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                            lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                        {
                            ...POST_DATA_2,
                            id: post2.id,
                            status: PostStatus.publish,
                            createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                            lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                    ],
                    total: 2,
                });
            });

            it('Возвращает посты с отложенной публикацией', async () => {
                const STATUS_DELAYED = 'delayed';
                const NEED_PUBLISH_AT = '2030-01-20T20:10:30.000Z';
                const post1 = await factory.createPost('Post', {
                    ...POST_DATA_1,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-20T20:10:30.000Z',
                });
                const post2 = await factory.createPost('Post', {
                    ...POST_DATA_2,
                    status: PostStatus.publish,
                    needPublishAt: NEED_PUBLISH_AT,
                    createdAt: '2021-01-10T20:10:30.000Z',
                });

                await factory.createManyPosts('Post', 3, { status: PostStatus.draft });

                const response = await request(server)
                    .get('/internal/posts')
                    .query({ status: STATUS_DELAYED, pageSize: 10 });

                expect(response.statusCode).toBe(200);
                expect(response.body).toEqual({
                    data: [
                        {
                            ...POST_DATA_1,
                            id: post1.id,
                            status: STATUS_DELAYED,
                            needPublishAt: NEED_PUBLISH_AT,
                            createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                            lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                        {
                            ...POST_DATA_2,
                            id: post2.id,
                            status: STATUS_DELAYED,
                            needPublishAt: NEED_PUBLISH_AT,
                            createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                            lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                            tags: [],
                            categories: [],
                        },
                    ],
                    total: 2,
                });
            });
        });

        it('Может фильтровать по автору', async () => {
            const AUTHOR = 'author123';
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                author: AUTHOR,
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                author: AUTHOR,
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3);

            const response = await request(server).get('/internal/posts').query({ author: AUTHOR, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        author: AUTHOR,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        author: AUTHOR,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по черновику заголовка и лиду блока', async () => {
            const TEXT = 'покупк';
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                draftTitle: 'Задумались о покупке квартиры?',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                lead: 'Покупка недвижимости - дело ответственное!',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createPost('Post', { draftTitle: 'Получение налогового вычета', draftBlocks: [] });
            await factory.createPost('Post', {
                draftTitle: 'Что важно для заключения сделки на вторичном рынке',
                draftBlocks: [],
            });

            const response = await request(server).get('/internal/posts').query({ textContains: TEXT, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        draftTitle: post1.draftTitle,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        lead: post2.lead,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по флагу скрытия из RSS', async () => {
            const RSS_OFF = true;
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                rssOff: RSS_OFF,
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                rssOff: RSS_OFF,
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, { rssOff: !RSS_OFF });

            const response = await request(server).get('/internal/posts').query({ rssOff: RSS_OFF, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        rssOff: post1.rssOff,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        rssOff: post2.rssOff,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по нижней дате создания', async () => {
            const post1 = await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            const post2 = await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-09T20:10:30.000Z' });

            const response = await request(server)
                .get('/internal/posts')
                .query({ createDateFrom: '2021-01-10T00:10:30.000Z', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по верхней дате создания', async () => {
            const post1 = await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            const post2 = await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T20:10:30.000Z' });

            await factory.createManyPosts('Post', 3, { createdAt: '2021-01-22T20:10:30.000Z' });

            const response = await request(server)
                .get('/internal/posts')
                .query({ createDateTo: '2021-01-22T00:10:30.000Z', pageSize: 10 });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по нижней дате публикации', async () => {
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                publishAt: '2021-01-30T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                publishAt: '2021-01-20T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-15T20:10:30.000Z',
                createdAt: '2021-01-09T20:10:30.000Z',
            });

            const response = await request(server)
                .get('/internal/posts')
                .query({ publishDateFrom: '2021-01-16T00:10:30.000Z', pageSize: 10 });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        publishAt: post1.publishAt && new Date(post1.publishAt).toISOString(),
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        publishAt: post2.publishAt && new Date(post2.publishAt).toISOString(),
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по верхней дате публикации', async () => {
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                publishAt: '2021-01-23T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });

            await factory.createManyPosts('Post', 3, {
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-22T20:10:30.000Z',
            });

            const response = await request(server)
                .get('/internal/posts')
                .query({ publishDateTo: '2021-01-25T00:10:30.000Z', pageSize: 10 });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        publishAt: post1.publishAt && new Date(post1.publishAt).toISOString(),
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        publishAt: post2.publishAt && new Date(post2.publishAt).toISOString(),
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может отдавать посты с указанным лимитом', async () => {
            const post1 = await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            const post2 = await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T20:10:30.000Z' });
            const post3 = await factory.createPost('Post', { ...POST_DATA_3, createdAt: '2021-01-09T20:10:30.000Z' });

            await factory.createPost('Post', { createdAt: '2021-01-01T17:10:30.000Z' });

            const response = await request(server).get('/internal/posts').query({ pageSize: 3 });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        tags: [],
                        categories: [],
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_2,
                        tags: [],
                        categories: [],
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                    },
                    {
                        ...POST_DATA_3,
                        tags: [],
                        categories: [],
                        createdAt: post3.createdAt && new Date(post3.createdAt).toISOString(),
                        lastEditedAt: post3.lastEditedAt && new Date(post3.lastEditedAt).toISOString(),
                    },
                ],
                total: 4,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может отдавать посты с указанным лимитом и страницей', async () => {
            await factory.createPost('Post', { createdAt: '2021-01-18T20:10:30.000Z' });
            await factory.createPost('Post', { createdAt: '2021-01-19T20:10:30.000Z' });
            await factory.createPost('Post', { createdAt: '2021-01-20T20:10:30.000Z' });
            const POST_DATA = { ...POST_DATA_1, createdAt: '2021-01-17T20:10:30.000Z' };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server).get('/internal/posts').query({ pageSize: 3, pageNumber: 1 });

            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA,
                        tags: [],
                        categories: [],
                        createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                        lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
                    },
                ],
                total: 4,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может сортировать посты по указанному полю', async () => {
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                publishAt: '2021-01-26T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-21T20:10:30.000Z',
            });
            const post3 = await factory.createPost('Post', {
                ...POST_DATA_3,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-22T20:10:30.000Z',
            });

            const response = await request(server).get('/internal/posts').query({ orderBySort: 'publishAt' });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        publishAt: post1.publishAt && new Date(post1.publishAt).toISOString(),
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        publishAt: post2.publishAt && new Date(post2.publishAt).toISOString(),
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_3,
                        publishAt: post3.publishAt && new Date(post3.publishAt).toISOString(),
                        createdAt: post3.createdAt && new Date(post3.createdAt).toISOString(),
                        lastEditedAt: post3.lastEditedAt && new Date(post3.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 3,
            });
        });

        it('Может сортировать посты в указанном направлении', async () => {
            const post1 = await factory.createPost('Post', {
                ...POST_DATA_1,
                publishAt: '2021-01-24T20:10:30.000Z',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            const post2 = await factory.createPost('Post', {
                ...POST_DATA_2,
                publishAt: '2021-01-25T20:10:30.000Z',
                createdAt: '2021-01-10T20:10:30.000Z',
            });
            const post3 = await factory.createPost('Post', {
                ...POST_DATA_3,
                publishAt: '2021-01-26T20:10:30.000Z',
                createdAt: '2021-01-09T20:10:30.000Z',
            });

            const response = await request(server)
                .get('/internal/posts')
                .query({ orderBySort: 'publishAt', orderByAsc: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...POST_DATA_1,
                        publishAt: post1.publishAt && new Date(post1.publishAt).toISOString(),
                        createdAt: post1.createdAt && new Date(post1.createdAt).toISOString(),
                        lastEditedAt: post1.lastEditedAt && new Date(post1.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_2,
                        publishAt: post2.publishAt && new Date(post2.publishAt).toISOString(),
                        createdAt: post2.createdAt && new Date(post2.createdAt).toISOString(),
                        lastEditedAt: post2.lastEditedAt && new Date(post2.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                    {
                        ...POST_DATA_3,
                        publishAt: post3.publishAt && new Date(post3.publishAt).toISOString(),
                        createdAt: post3.createdAt && new Date(post3.createdAt).toISOString(),
                        lastEditedAt: post3.lastEditedAt && new Date(post3.lastEditedAt).toISOString(),
                        tags: [],
                        categories: [],
                    },
                ],
                total: 3,
            });
        });

        it('Возвращает data = [] и total = 0, если посты не найдены', async () => {
            await factory.createPost('Post', { ...POST_DATA_1, createdAt: '2021-01-20T20:10:30.000Z' });
            await factory.createPost('Post', { ...POST_DATA_2, createdAt: '2021-01-10T20:10:30.000Z' });

            const response = await request(server)
                .get('/internal/posts')
                .query({ createDateTo: '1999-01-10T20:10:30.000Z', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [],
                total: 0,
            });
        });
    });

    describe('GET /internal/posts/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).get(`/internal/posts/${NON_EXISTING_POST_ID}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает пост с неархивными тегами и рубриками', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false });
            const tag2 = await factory.createTag('Tag', { isArchived: true });
            const category1 = await factory.createCategory('Category', { isArchived: false });
            const category2 = await factory.createCategory('Category', { isArchived: true });

            const POST_DATA = { ...POST_DATA_1 };
            const post = await factory.createPost('Post', POST_DATA).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                await model.$add('categories', [category1, category2]);
                return model;
            });

            const response = await request(server)
                .get(`/internal/posts/${POST_DATA.urlPart}`)
                .query({ withArchived: 'false' });

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                authors: [],
                tags: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                ],
                categories: [
                    {
                        ...category1.toJSON(),
                        createdAt: new Date(category1.createdAt).toISOString(),
                        lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                    },
                ],
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост со всеми тегами и рубриками', async () => {
            const tag1 = await factory.createTag('Tag', { urlPart: 'arenda', isArchived: false });
            const tag2 = await factory.createTag('Tag', { urlPart: 'ipoteka', isArchived: true });
            const category1 = await factory.createCategory('Category', { urlPart: 'arenda', isArchived: false });
            const category2 = await factory.createCategory('Category', { urlPart: 'ipoteka', isArchived: true });

            const POST_DATA = { ...POST_DATA_1 };
            const post = await factory.createPost('Post', POST_DATA).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                await model.$add('categories', [category1, category2]);
                return model;
            });

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                authors: [],
                tags: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                ],
                categories: [
                    {
                        ...category1.toJSON(),
                        createdAt: new Date(category1.createdAt).toISOString(),
                        lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                    },
                    {
                        ...category2.toJSON(),
                        createdAt: new Date(category2.createdAt).toISOString(),
                        lastEditedAt: new Date(category2.lastEditedAt).toISOString(),
                    },
                ],
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с выбранной предыдущей статьей', async () => {
            const POST_BEFORE_DATA = { ...POST_DATA_1, status: PostStatus.publish };
            const POST_DATA = { ...POST_DATA_2, before: POST_BEFORE_DATA.urlPart };

            const postBefore = await factory.createPost('Post', POST_BEFORE_DATA);
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                _before: {
                    ...POST_BEFORE_DATA,
                    id: postBefore.id,
                    tags: [],
                    categories: [],
                    authors: [],
                    createdAt: postBefore.createdAt && new Date(postBefore.createdAt).toISOString(),
                    lastEditedAt: postBefore.lastEditedAt && new Date(postBefore.lastEditedAt).toISOString(),
                },
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с предыдущей статьей по категории, если выбранная статья не опубликована', async () => {
            const CATEGORY_POST_DATA = { ...POST_DATA_1, publishAt: '2021-01-20T20:10:30.000Z' };
            const POST_BEFORE_DATA = { ...POST_DATA_2, status: PostStatus.draft };
            const POST_DATA = {
                ...POST_DATA_3,
                before: POST_BEFORE_DATA.urlPart,
                publishAt: '2021-01-21T20:10:30.000Z',
            };

            await factory.createPost('Post', POST_BEFORE_DATA);
            const post = await factory.createPost('Post', POST_DATA);
            const category = await factory.createCategory('Category');
            const categoryPost = await factory.createPost('Post', CATEGORY_POST_DATA).then(async model => {
                await model.$add('category', category);
                return model;
            });

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                _before: {
                    ...CATEGORY_POST_DATA,
                    id: categoryPost.id,
                    tags: [],
                    authors: [],
                    categories: [
                        {
                            ...category.toJSON(),
                            createdAt: new Date(category.createdAt).toISOString(),
                            lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                        },
                    ],
                    createdAt: categoryPost.createdAt && new Date(categoryPost.createdAt).toISOString(),
                    lastEditedAt: categoryPost.lastEditedAt && new Date(categoryPost.lastEditedAt).toISOString(),
                },
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с пустой предыдущей статьей', async () => {
            const POST_DATA = { ...POST_DATA_2 };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с выбранной следующей статьей', async () => {
            const POST_AFTER_DATA = { ...POST_DATA_1, status: PostStatus.publish };
            const POST_DATA = { ...POST_DATA_2, after: POST_AFTER_DATA.urlPart };

            const postAfter = await factory.createPost('Post', POST_AFTER_DATA);
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                _after: {
                    ...POST_AFTER_DATA,
                    id: postAfter.id,
                    tags: [],
                    categories: [],
                    authors: [],
                    createdAt: postAfter.createdAt && new Date(postAfter.createdAt).toISOString(),
                    lastEditedAt: postAfter.lastEditedAt && new Date(postAfter.lastEditedAt).toISOString(),
                },
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост со следующей статьей по категории, если выбранная статья не опубликована', async () => {
            const CATEGORY_POST_DATA = { ...POST_DATA_1, publishAt: '2021-01-22T20:10:30.000Z' };
            const POST_AFTER_DATA = { ...POST_DATA_2, status: PostStatus.draft };
            const POST_DATA = { ...POST_DATA_3, after: POST_AFTER_DATA.urlPart, publishAt: '2021-01-21T20:10:30.000Z' };

            await factory.createPost('Post', POST_AFTER_DATA);
            const post = await factory.createPost('Post', POST_DATA);
            const category = await factory.createCategory('Category');
            const categoryPost = await factory.createPost('Post', CATEGORY_POST_DATA).then(async model => {
                await model.$add('category', category);
                return model;
            });

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                _after: {
                    ...CATEGORY_POST_DATA,
                    id: categoryPost.id,
                    tags: [],
                    authors: [],
                    categories: [
                        {
                            ...category.toJSON(),
                            createdAt: new Date(category.createdAt).toISOString(),
                            lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                        },
                    ],
                    createdAt: categoryPost.createdAt && new Date(categoryPost.createdAt).toISOString(),
                    lastEditedAt: categoryPost.lastEditedAt && new Date(categoryPost.lastEditedAt).toISOString(),
                },
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с пустой следующей статьей', async () => {
            const POST_DATA = { ...POST_DATA_2 };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server).get(`/internal/posts/${POST_DATA.urlPart}`);

            expect(response.body).toEqual({
                ...POST_DATA,
                id: post.id,
                tags: [],
                categories: [],
                authors: [],
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: post.lastEditedAt && new Date(post.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с авторами', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POST.$add('authors', AUTHORS);

            const response = await request(server).get(`/internal/posts/${POST.urlPart}`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пост с микроразметкой FAQPage', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await PostSchemaMarkupModel.create({ postId: POST.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES });

            const response = await request(server).get(`/internal/posts/${POST.urlPart}`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('GET /internal/posts/minimized/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).get(`/internal/posts/minimized/${NON_EXISTING_POST_ID}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает urlPart, title, status, mainImage и needPublishAt поста', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            await PostModel.create(POST_ATTRIBUTES);

            const response = await request(server).get(`/internal/posts/minimized/${POST_ATTRIBUTES.urlPart}`);

            expect(response.body).toEqual({
                urlPart: 'posmotrite-na-ochen-krutuyu-videoreklamu-jeep-wrangler',
                title: 'Посмотрите на очень крутую видеорекламу Jeep Wrangler ',
                status: PostStatus.publish,
                mainImage: POST_ATTRIBUTES.mainImage,
                needPublishAt: '2021-09-02T14:00:00.000Z',
            });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/posts', function () {
        it('Создает и возвращает новый пост', async () => {
            const { AUTHOR_ATTRIBUTES } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_ATTRIBUTES);
            const beforePost = await factory.createPost('Post');
            const afterPost = await factory.createPost('Post');
            const tag = await factory.createTag('Tag');
            const category = await factory.createCategory('Category');
            const POST_DATA = {
                ...CREATE_POST_DATA_1,
                before: beforePost.urlPart,
                after: afterPost.urlPart,
                tags: [tag.urlPart],
                categories: [category.urlPart],
                authors: [author.id],
            };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...POST_DATA,
                lastEditLogin: POST_DATA.author,
                tags: [
                    {
                        urlPart: tag.urlPart,
                        service: tag.service,
                        title: tag.title,
                        shortTitle: tag.shortTitle,
                        blocks: tag.blocks,
                        isArchived: tag.isArchived,
                        isHot: tag.isHot,
                        isPartnership: tag.isPartnership,
                        metaTitle: tag.metaTitle,
                        metaDescription: tag.metaDescription,
                        mmm: tag.mmm,
                        createdAt: new Date(tag.createdAt).toISOString(),
                        lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    },
                ],
                categories: [
                    {
                        ...category.toJSON(),
                        createdAt: new Date(category.createdAt).toISOString(),
                        lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                    },
                ],
                authors: [
                    {
                        ...author.toJSON(),
                        createdAt: new Date(author.createdAt).toISOString(),
                        lastEditedAt: new Date(author.lastEditedAt).toISOString(),
                    },
                ],
            });
            await expect(PostModel.findByPk(POST_DATA.urlPart)).resolves.toBeTruthy();
        });

        it('Создает и возвращает новый пост, тримая и приводя urlPart к нижнему регистру', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const response = await request(server).post('/internal/posts').send(POST_ATTRIBUTES);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
            await expect(PostModel.findByPk(response.body.urlPart)).resolves.toBeTruthy();
        });

        it('Возвращает ошибку и статус 400, если не задан "urlPart"', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, urlPart: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать "urlPart"',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик заголовка', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, draftTitle: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать заголовок',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан лид', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, lead: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать лид',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик блоков', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, draftBlocks: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать блоки',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик основного изображения', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, draftMainImage: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать основное изображение',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указан автор', async () => {
            const POST_DATA = { ...CREATE_POST_DATA_1, author: undefined };

            const response = await request(server).post('/internal/posts').send(POST_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать автора',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если пост с таким urlPart уже существует', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

            await PostModel.create(POST_ATTRIBUTES_2);

            const response = await request(server).post('/internal/posts').send(POST_ATTRIBUTES_1);

            expect(response.body).toEqual({
                status: 400,
                error: 'Пост с таким "urlPart" уже существует',
            });
            expect(response.statusCode).toBe(400);
        });
    });

    describe('PUT /internal/posts/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server)
                .put(`/internal/posts/${NON_EXISTING_POST_ID}`)
                .send(UPDATE_POST_DATA_1);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик заголовка', async () => {
            const POST = await factory.createPost('Post', CREATE_POST_DATA_1);
            const UPDATE_DATA = { ...UPDATE_POST_DATA_1, draftTitle: undefined };

            const response = await request(server).put(`/internal/posts/${POST.urlPart}`).send(UPDATE_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать заголовок',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан лид', async () => {
            const POST = await factory.createPost('Post', CREATE_POST_DATA_1);
            const UPDATE_DATA = { ...UPDATE_POST_DATA_1, lead: undefined };

            const response = await request(server).put(`/internal/posts/${POST.urlPart}`).send(UPDATE_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать лид',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик блоков', async () => {
            const POST = await factory.createPost('Post', CREATE_POST_DATA_1);
            const UPDATE_DATA = { ...UPDATE_POST_DATA_1, draftBlocks: undefined };

            const response = await request(server).put(`/internal/posts/${POST.urlPart}`).send(UPDATE_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать блоки',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не задан черновик основного изображения', async () => {
            const POST = await factory.createPost('Post', CREATE_POST_DATA_1);
            const UPDATE_DATA = { ...UPDATE_POST_DATA_1, draftMainImage: undefined };

            const response = await request(server).put(`/internal/posts/${POST.urlPart}`).send(UPDATE_DATA);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать основное изображение',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если существует другой пост с новым urlPart', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, UPDATE_ATTRIBUTES } = getFixtures(fixtures);

            await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2]);

            const response = await request(server)
                .put(`/internal/posts/${POST_ATTRIBUTES_1.urlPart}`)
                .send(UPDATE_ATTRIBUTES);

            expect(response.body).toEqual({
                status: 400,
                error: 'Существует другой пост с таким "urlPart"',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Обновляет и возвращает модель поста', async () => {
            const beforePost = await factory.createPost('Post');
            const afterPost = await factory.createPost('Post');
            const updateBeforePost = await factory.createPost('Post');
            const updateAfterPost = await factory.createPost('Post');
            const post = await factory.createPost('Post', {
                ...POST_DATA_1,
                before: beforePost.urlPart,
                after: afterPost.urlPart,
            });

            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                before: updateBeforePost.urlPart,
                after: updateAfterPost.urlPart,
            };

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                blocks: post.blocks,
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });

        it('Обновляет и возвращает модель поста, тримая и приводя urlPart к нижнему регистру', async () => {
            const { POST_ATTRIBUTES, UPDATE_ATTRIBUTES } = getFixtures(fixtures);

            await PostModel.create(POST_ATTRIBUTES);

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementationOnce(() => new Date(UPDATE_DATE));

            const response = await request(server)
                .put(`/internal/posts/${POST_ATTRIBUTES.urlPart}`)
                .send(UPDATE_ATTRIBUTES);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Добавляет теги и возвращает модель поста', async () => {
            const tag1 = await factory.createTag('Tag', { urlPart: 'arenda' });
            const tag2 = await factory.createTag('Tag', { urlPart: 'ipoteka' });
            const CREATE_DATA = { ...CREATE_POST_DATA_1 };
            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                tags: [tag1.urlPart, tag2.urlPart],
            };
            const post = await factory.createPost('Post', CREATE_DATA);

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                tags: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        lastEditedAt: new Date(tag1.createdAt).toISOString(),
                        createdAt: new Date(tag1.createdAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag1.blocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        lastEditedAt: new Date(tag2.createdAt).toISOString(),
                        createdAt: new Date(tag2.createdAt).toISOString(),
                    },
                ],
            });
        });

        it('Обновляет теги и возвращает модель поста', async () => {
            const tag1 = await factory.createTag('Tag', { urlPart: 'arenda' });
            const tag2 = await factory.createTag('Tag', { urlPart: 'ipoteka' });
            const tag3 = await factory.createTag('Tag', { urlPart: 'kommercheskaya-nedvizhimost' });
            const CREATE_DATA = { ...CREATE_POST_DATA_1 };
            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                tags: [tag2.urlPart, tag3.urlPart],
            };
            const post = await factory.createPost('Post', CREATE_DATA).then(async model => {
                await model.$add('tags', [tag1.urlPart, tag2.urlPart]);
                return model;
            });

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                tags: [
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag1.blocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        lastEditedAt: new Date(tag2.createdAt).toISOString(),
                        createdAt: new Date(tag2.createdAt).toISOString(),
                    },
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag1.blocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        lastEditedAt: new Date(tag3.createdAt).toISOString(),
                        createdAt: new Date(tag3.createdAt).toISOString(),
                    },
                ],
            });
        });

        it('Удаляет теги и возвращает модель поста', async () => {
            const tag1 = await factory.createTag('Tag', { urlPart: 'arenda' });
            const tag2 = await factory.createTag('Tag', { urlPart: 'ipoteka' });
            const CREATE_DATA = { ...CREATE_POST_DATA_1 };
            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                tags: [],
            };
            const post = await factory.createPost('Post', CREATE_DATA).then(async model => {
                await model.$add('tags', [tag1.urlPart, tag2.urlPart]);
                return model;
            });

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                tags: [],
            });
        });

        it('Добавляет категории и возвращает модель поста', async () => {
            const category1 = await factory.createCategory('Category', { urlPart: 'arenda' });
            const category2 = await factory.createCategory('Category', { urlPart: 'ipoteka' });
            const CREATE_DATA = { ...CREATE_POST_DATA_1 };
            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                categories: [category1.urlPart, category2.urlPart],
            };
            const post = await factory.createPost('Post', CREATE_DATA);

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                categories: [
                    {
                        ...category1.toJSON(),
                        lastEditedAt: new Date(category1.createdAt).toISOString(),
                        createdAt: new Date(category1.createdAt).toISOString(),
                    },
                    {
                        ...category2.toJSON(),
                        lastEditedAt: new Date(category2.createdAt).toISOString(),
                        createdAt: new Date(category2.createdAt).toISOString(),
                    },
                ],
            });
        });

        it('Обновляет категории и возвращает модель поста', async () => {
            const category1 = await factory.createCategory('Category', { urlPart: 'arenda' });
            const category2 = await factory.createCategory('Category', { urlPart: 'ipoteka' });
            const category3 = await factory.createCategory('Category', { urlPart: 'kommercheskaya-nedvizhimost' });
            const CREATE_DATA = { ...CREATE_POST_DATA_1 };
            const UPDATE_DATA = {
                ...UPDATE_POST_DATA_1,
                categories: [category2.urlPart, category3.urlPart],
            };
            const post = await factory.createPost('Post', CREATE_DATA).then(async model => {
                await model.$add('categories', [category1.urlPart, category2.urlPart]);
                return model;
            });

            const response = await request(server).put(`/internal/posts/${CREATE_DATA.urlPart}`).send(UPDATE_DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                categories: [
                    {
                        ...category2.toJSON(),
                        lastEditedAt: new Date(category2.createdAt).toISOString(),
                        createdAt: new Date(category2.createdAt).toISOString(),
                    },
                    {
                        ...category3.toJSON(),
                        lastEditedAt: new Date(category3.createdAt).toISOString(),
                        createdAt: new Date(category3.createdAt).toISOString(),
                    },
                ],
            });
        });

        it('Удаляет категории и возвращает модель поста', async () => {
            const category1 = await factory.createCategory('Category', { urlPart: 'arenda' });
            const category2 = await factory.createCategory('Category', { urlPart: 'ipoteka' });
            const post = await factory.createPost('Post', CREATE_POST_DATA_1).then(async model => {
                await model.$add('categories', [category1.urlPart, category2.urlPart]);
                return model;
            });
            const UPDATE_DATA = { ...UPDATE_POST_DATA_1, categories: [] };

            const response = await request(server).put(`/internal/posts/${post.urlPart}`).send(UPDATE_DATA);

            expect(response.body).toMatchObject({
                ...UPDATE_DATA,
                urlPart: UPDATE_DATA.urlPart.trim().toLowerCase(),
                author: post.author,
                title: post.title,
                titleRss: post.titleRss,
                titleApp: post.titleApp,
                createdAt: post.createdAt && post.createdAt && new Date(post.createdAt).toISOString(),
                lastEditedAt: expect.anything(),
                categories: [],
            });
            expect(response.statusCode).toBe(200);
        });

        it('Обновляет авторов и возвращает модель поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2, AUTHOR_ATTRIBUTES_3 } =
                getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const AUTHORS = await AuthorModel.bulkCreate([
                AUTHOR_ATTRIBUTES_1,
                AUTHOR_ATTRIBUTES_2,
                AUTHOR_ATTRIBUTES_3,
            ]);

            await POST.$add('authors', AUTHORS);

            const response = await request(server)
                .put(`/internal/posts/${POST.urlPart}`)
                .send({
                    ...POST_ATTRIBUTES,
                    authors: [AUTHORS[0]?.id, AUTHORS[2]?.id],
                });

            expect(response.body).toMatchSnapshot();
            expect(response.status).toBe(200);
        });
    });

    describe('POST /internal/posts/:urlPart/publish', function () {
        it('Возвращает ошибку и статус 400, если не передать логин юзера', async () => {
            const POST = await factory.createPost('Post', { status: PostStatus.draft });

            const response = await request(server).post(`/internal/posts/${POST.urlPart}/publish`);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать логин пользователя',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).post(`/internal/posts/${NON_EXISTING_POST_ID}/publish`).send({
                userLogin: 'swapster',
            });

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Публикует пост и возвращает его модель', async () => {
            const PUBLISH_AUTHOR = 'user123';
            const POST_DATA = { ...PUBLISH_POST_DATA_1 };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server)
                .post(`/internal/posts/${post.urlPart}/publish`)
                .send({ userLogin: PUBLISH_AUTHOR });

            expect(response.body).toMatchObject({
                ...POST_DATA,
                status: 'publish',
                publishAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Ставит пост в статус отложенной публикации, если указана дата публикации', async () => {
            const PUBLISH_AUTHOR = 'user123';
            const PUBLISH_DATE = '2031-01-20T20:10:30.000Z';
            const POST_DATA = { ...PUBLISH_POST_DATA_1 };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server)
                .post(`/internal/posts/${post.urlPart}/publish`)
                .send({ needPublishAt: PUBLISH_DATE, userLogin: PUBLISH_AUTHOR });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                ...POST_DATA,
                status: 'delayed',
                needPublishAt: '2031-01-20T20:10:30.000Z',
                publishAt: '2031-01-20T20:10:30.000Z',
            });
        });
    });

    describe('POST /internal/posts/:urlPart/unpublish', function () {
        it('Возвращает ошибку и статус 400, если не передать логин юзера', async () => {
            const POST = await factory.createPost('Post', { status: PostStatus.draft });

            const response = await request(server).post(`/internal/posts/${POST.urlPart}/unpublish`);

            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать логин пользователя',
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).post(`/internal/posts/${NON_EXISTING_POST_ID}/unpublish`).send({
                userLogin: 'swapster',
            });

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Снимает пост с публикации и возвращает его модель', async () => {
            const UNPUBLISH_AUTHOR = 'user123';
            const post = await factory.createPost('Post', {
                ...UNPUBLISH_POST_DATA_1,
                status: PostStatus.publish,
            });

            const response = await request(server)
                .post(`/internal/posts/${post.urlPart}/unpublish`)
                .send({ userLogin: UNPUBLISH_AUTHOR });

            expect(response.body).toMatchObject({
                ...UNPUBLISH_POST_DATA_1,
                status: PostStatus.closed,
            });
            expect(response.body.publishAt).toBeUndefined();
            expect(response.body.needPublishAt).toBeUndefined();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/posts/:urlPart/remove', function () {
        it('Удаляет существующий черновой пост', async () => {
            const REMOVE_AUTHOR = 'user123';
            const post = await factory.createPost('Post', { status: PostStatus.draft });

            const removeResponse = await request(server)
                .post(`/internal/posts/${post.urlPart}/remove`)
                .send({ userLogin: REMOVE_AUTHOR });
            const findResponse = await request(server).get(`/internal/posts/${post.urlPart}`);

            expect(removeResponse.statusCode).toBe(200);
            expect(removeResponse.body).toMatchObject({
                urlPart: post.urlPart,
            });
            expect(findResponse.body).toMatchObject({
                error: 'Пост не найден',
            });
        });

        it('Удаляет существующий опубликованный пост', async () => {
            const REMOVE_AUTHOR = 'user123';
            const post = await factory.createPost('Post', { status: PostStatus.publish });

            const removeResponse = await request(server)
                .post(`/internal/posts/${post.urlPart}/remove`)
                .send({ userLogin: REMOVE_AUTHOR });
            const findResponse = await request(server).get(`/internal/posts/${post.urlPart}`);

            expect(removeResponse.statusCode).toBe(200);
            expect(removeResponse.body).toMatchObject({
                urlPart: post.urlPart,
            });
            expect(findResponse.body).toMatchObject({
                error: 'Пост не найден',
            });
        });

        it('Удаление несуществующего поста', async () => {
            const REMOVE_AUTHOR = 'user123';
            const urlPart = 'i-do-not-exist';

            const response = await request(server)
                .post(`/internal/posts/${urlPart}/remove`)
                .send({ userLogin: REMOVE_AUTHOR });

            expect(response.statusCode).toBe(404);
            expect(response.body).toMatchObject({
                error: 'Пост не найден',
            });
        });

        it('Удаляет пост со связью тег/категория', async () => {
            const REMOVE_AUTHOR = 'user123';
            const tag = await factory.createTag('Tag');
            const category = await factory.createCategory('Category');
            const post = await factory.createPost('Post', { status: PostStatus.publish }).then(async model => {
                await model.$add('tags', [tag]);
                return model;
            });
            const post2 = await factory.createPost('Post', { status: PostStatus.publish }).then(async model => {
                await model.$add('categories', [category]);
                return model;
            });

            const response = await request(server)
                .post(`/internal/posts/${post.urlPart}/remove`)
                .send({ userLogin: REMOVE_AUTHOR });
            const response2 = await request(server)
                .post(`/internal/posts/${post2.urlPart}/remove`)
                .send({ userLogin: REMOVE_AUTHOR });

            expect(response.statusCode).toBe(200);
            expect(response.body).toMatchObject({
                urlPart: post.urlPart,
            });
            expect(response2.statusCode).toBe(200);
            expect(response2.body).toMatchObject({
                urlPart: post2.urlPart,
            });
        });
    });

    describe('POST /internal/posts/:urlPart/commitDraft', function () {
        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server).post(`/internal/posts/${NON_EXISTING_POST_ID}/commitDraft`);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Копирует черновые поля в основные и возвращает пост', async () => {
            const AUTHOR = 'user123';
            const POST_DATA = { ...COMMIT_DRAFT_POST_DATA_1 };
            const post = await factory.createPost('Post', POST_DATA);

            const response = await request(server)
                .post(`/internal/posts/${post.urlPart}/commitDraft`)
                .send({ userLogin: AUTHOR });

            expect(response.body).toMatchObject({
                ...POST_DATA,
                title: post.draftTitle,
                titleRss: post.draftTitleRss,
                titleApp: post.draftTitleApp,
                blocks: post.draftBlocks,
                mainImage: post.draftMainImage,
                mainImage4x3: post.draftMainImage4x3,
                mainImageRss: post.draftMainImageRss,
                mainImageApp: post.draftMainImageApp,
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('PUT /internal/posts/:urlPart/markUpdated', function () {
        beforeEach(() => {
            Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
        });

        it('Возвращает ошибку и статус 404, если пост не найден', async () => {
            const NON_EXISTING_POST_ID = 'non-existing-post-id';

            const response = await request(server)
                .put(`/internal/posts/${NON_EXISTING_POST_ID}/markUpdated`)
                .send(UPDATE_POST_DATA_1);

            expect(response.body).toEqual({
                status: 404,
                error: 'Пост не найден',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Если пост еще не редактировали, то его можно пометить текущим пользователем', async () => {
            const EDITOR_LOGIN = 'editor-1';
            const POST = await factory.createPost('Post', { lastOnlineLogin: null, lastOnlineTime: null });

            const response = await request(server)
                .put(`/internal/posts/${POST.urlPart}/markUpdated`)
                .send({ lastOnlineLogin: EDITOR_LOGIN });

            expect(response.body).toEqual({
                lastOnlineTime: DATE_NOW,
                lastOnlineLogin: EDITOR_LOGIN,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Если пост недавно редактировался другим пользователем, то возвращается ошибка и сведения о редактировании другим пользователем', async () => {
            const EDITOR_LOGIN_1 = 'editor-1';
            const EDITOR_LOGIN_2 = 'editor-2';
            const LAST_ONLINE_TIME = '2021-09-08T12:29:50.000Z';
            const POST = await factory.createPost('Post', {
                lastOnlineLogin: EDITOR_LOGIN_1,
                lastOnlineTime: '2021-09-08T12:29:50.000Z',
            });

            const response = await request(server)
                .put(`/internal/posts/${POST.urlPart}/markUpdated`)
                .send({ lastOnlineLogin: EDITOR_LOGIN_2 });

            expect(response.body).toEqual({
                error: 'Пост сейчас редактируют',
                lastOnlineTime: LAST_ONLINE_TIME,
                lastOnlineLogin: EDITOR_LOGIN_1,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Если пост недавно редактировался другим пользователем, то его можно перехватить и пометить текущим пользователем', async () => {
            const EDITOR_LOGIN_1 = 'editor-1';
            const EDITOR_LOGIN_2 = 'editor-2';
            const LAST_ONLINE_TIME = '2021-09-08T12:29:50.000Z';
            const POST = await factory.createPost('Post', {
                lastOnlineLogin: EDITOR_LOGIN_1,
                lastOnlineTime: LAST_ONLINE_TIME,
            });

            const response = await request(server).put(`/internal/posts/${POST.urlPart}/markUpdated`).send({
                lastOnlineLogin: EDITOR_LOGIN_2,
                force: true,
            });

            expect(response.body).toEqual({
                lastOnlineTime: DATE_NOW,
                lastOnlineLogin: EDITOR_LOGIN_2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Если пост давно редактировался другим пользователем, то помечаем его текущим пользователем', async () => {
            const EDITOR_LOGIN_1 = 'editor-1';
            const EDITOR_LOGIN_2 = 'editor-2';
            const OLD_LAST_ONLINE_TIME = '2021-08-12T12:20:50.000Z';
            const POST = await factory.createPost('Post', {
                lastOnlineLogin: EDITOR_LOGIN_1,
                lastOnlineTime: OLD_LAST_ONLINE_TIME,
            });

            const response = await request(server).put(`/internal/posts/${POST.urlPart}/markUpdated`).send({
                lastOnlineLogin: EDITOR_LOGIN_2,
            });

            expect(response.body).toEqual({
                lastOnlineTime: DATE_NOW,
                lastOnlineLogin: EDITOR_LOGIN_2,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Если пост редактировался тем же пользователем, то ставим новый таймстемп обновления', async () => {
            const EDITOR_LOGIN = 'editor-1';
            const OLD_LAST_ONLINE_TIME = '2021-08-12T12:20:50.000Z';
            const POST = await factory.createPost('Post', {
                lastOnlineLogin: EDITOR_LOGIN,
                lastOnlineTime: OLD_LAST_ONLINE_TIME,
            });

            const response = await request(server).put(`/internal/posts/${POST.urlPart}/markUpdated`).send({
                lastOnlineLogin: EDITOR_LOGIN,
            });

            expect(response.body).toEqual({
                lastOnlineTime: DATE_NOW,
                lastOnlineLogin: EDITOR_LOGIN,
            });
            expect(response.statusCode).toBe(200);
        });
    });
});
