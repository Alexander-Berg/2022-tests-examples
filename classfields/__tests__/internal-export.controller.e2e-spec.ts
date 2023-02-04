import { INestApplication } from '@nestjs/common';
import request from 'supertest';

import 'jest-extended';
import { FactoryService } from '../../../services/factory.service';
import { PostStatus } from '../../../types/post';
import { ExportType } from '../../../types/export';
import { Service } from '../../../types/common';
import { createTestingApp } from '../../../tests/app';

describe('Internal exports controller', () => {
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

    describe('GET /internal/exports', function () {
        it('Возвращает модели экспорта отсортированные по возрастанию типа и порядка и по убыванию даты создания', async () => {
            const exports = await factory
                .createManyExports('Export', 5, [
                    { type: ExportType.desktop, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 0, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 1, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.morda, order: 0, createdAt: new Date('2021-02-20T21:10:30.000Z') },
                    { type: ExportType.morda, order: 0, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                ])
                .then(models =>
                    models.map(model => ({
                        ...model.toJSON(),
                        posts: [],
                        categories: [],
                        tags: [],
                        createdAt: new Date(model.createdAt).toISOString(),
                        lastEditedAt: new Date(model.lastEditedAt).toISOString(),
                    }))
                );

            const response = await request(server).get('/internal/exports');

            expect(response.body).toEqual(exports);
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает экспорты с сортированными постами по order', async () => {
            const post1 = await factory.createPost('Post', { status: PostStatus.publish });
            const post2 = await factory.createPost('Post', { status: PostStatus.publish });
            const post3 = await factory.createPost('Post', { status: PostStatus.publish });
            const post4 = await factory.createPost('Post', { status: PostStatus.publish });
            const exportModel = await factory.createExport('Export');

            await factory.createManyExportPosts('ExportPost', 4, [
                { post_key: post1.urlPart, export_key: exportModel.id, order: 4 },
                { post_key: post2.urlPart, export_key: exportModel.id, order: 2 },
                { post_key: post3.urlPart, export_key: exportModel.id, order: 3 },
                { post_key: post4.urlPart, export_key: exportModel.id, order: 1 },
            ]);

            const response = await request(server).get('/internal/exports');

            expect(response.body[0].posts[0]).toMatchObject({ urlPart: post4.urlPart, export_post: { order: 1 } });
            expect(response.body[0].posts[3]).toMatchObject({ urlPart: post1.urlPart, export_post: { order: 4 } });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает экспорты с неархивными тегами/рубриками и опубликованными постами', async () => {
            const tag = await factory.createTag('Tag', { isArchived: false });
            const archivedTag = await factory.createTag('Tag', { isArchived: true });
            const category = await factory.createCategory('Category', { isArchived: false });
            const archivedCategory = await factory.createCategory('Category', { isArchived: true });
            const post = await factory.createPost('Post', { status: PostStatus.publish });
            const draftPost = await factory.createPost('Post', { status: PostStatus.draft });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', [tag, archivedTag]);
                await model.$add('categories', [category, archivedCategory]);
                await model.$add('posts', [post, draftPost]);
                return model;
            });

            const response = await request(server).get('/internal/exports');

            expect(response.body).toBeArrayOfSize(1);

            const { posts, categories, tags, ...exportData } = response.body[0];

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(categories).toEqual([
                {
                    ...category.toJSON(),
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                },
            ]);
            expect(tags).toEqual([
                {
                    urlPart: tag.urlPart,
                    service: tag.service,
                    title: tag.title,
                    shortTitle: tag.shortTitle,
                    blocks: tag.blocks,
                    draftTitle: tag.draftTitle,
                    draftShortTitle: tag.draftShortTitle,
                    draftBlocks: tag.draftBlocks,
                    isArchived: tag.isArchived,
                    isHot: tag.isHot,
                    isPartnership: tag.isPartnership,
                    metaTitle: tag.metaTitle,
                    metaDescription: tag.metaDescription,
                    mmm: tag.mmm,
                    createdAt: new Date(tag.createdAt).toISOString(),
                    lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    partnershipLink: tag.partnershipLink,
                    partnershipName: tag.partnershipName,
                    partnershipBadgeName: tag.partnershipBadgeName,
                },
            ]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post.urlPart);
            expect(response.statusCode).toBe(200);
        });

        it('Может возвращать экспорты с архивными тегами/рубриками и неопубликованными постами', async () => {
            const tag = await factory.createTag('Tag', { isArchived: true });
            const category = await factory.createCategory('Category', { isArchived: true });
            const post = await factory.createPost('Post', { status: PostStatus.draft });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag);
                await model.$add('categories', category);
                await model.$add('posts', post);
                return model;
            });

            const response = await request(server).get('/internal/exports').query({ withArchived: true });

            expect(response.body).toBeArrayOfSize(1);

            const { posts, categories, tags, ...exportData } = response.body[0];

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(categories).toEqual([
                {
                    ...category.toJSON(),
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                },
            ]);
            expect(tags).toEqual([
                {
                    urlPart: tag.urlPart,
                    service: tag.service,
                    title: tag.title,
                    shortTitle: tag.shortTitle,
                    blocks: tag.blocks,
                    draftTitle: tag.draftTitle,
                    draftShortTitle: tag.draftShortTitle,
                    draftBlocks: tag.draftBlocks,
                    isArchived: tag.isArchived,
                    isHot: tag.isHot,
                    isPartnership: tag.isPartnership,
                    metaTitle: tag.metaTitle,
                    metaDescription: tag.metaDescription,
                    mmm: tag.mmm,
                    createdAt: new Date(tag.createdAt).toISOString(),
                    lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    partnershipLink: tag.partnershipLink,
                    partnershipName: tag.partnershipName,
                    partnershipBadgeName: tag.partnershipBadgeName,
                },
            ]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post.urlPart);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по сервису', async () => {
            await factory.createExport('Export', { service: Service.realty });
            const exportModel2 = await factory.createExport('Export', { service: Service.autoru });
            const exportModel3 = await factory.createExport('Export', { service: Service.autoru });

            const response = await request(server).get('/internal/exports').query({ service: Service.autoru });

            expect(response.body).toIncludeSameMembers([
                {
                    ...exportModel2.toJSON(),
                    posts: [],
                    categories: [],
                    tags: [],
                    createdAt: new Date(exportModel2.createdAt).toISOString(),
                    lastEditedAt: new Date(exportModel2.lastEditedAt).toISOString(),
                },
                {
                    ...exportModel3.toJSON(),
                    posts: [],
                    categories: [],
                    tags: [],
                    createdAt: new Date(exportModel3.createdAt).toISOString(),
                    lastEditedAt: new Date(exportModel3.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по типу', async () => {
            await factory.createExport('Export', { type: ExportType.morda });
            await factory.createExport('Export', { type: ExportType.desktop });
            const exportModel = await factory.createExport('Export', { type: ExportType.mobile });

            const response = await request(server).get('/internal/exports').query({ type: ExportType.mobile });

            expect(response.body).toEqual([
                {
                    ...exportModel.toJSON(),
                    posts: [],
                    categories: [],
                    tags: [],
                    createdAt: new Date(exportModel.createdAt).toISOString(),
                    lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по названию секции', async () => {
            await factory.createExport('Export', { section: 'Новинки' });
            await factory.createExport('Export', { section: 'Обзоры' });
            const exportModel = await factory.createExport('Export', { section: 'Новости' });

            const response = await request(server).get('/internal/exports').query({ section: 'Новости' });

            expect(response.body).toEqual([
                {
                    ...exportModel.toJSON(),
                    posts: [],
                    categories: [],
                    tags: [],
                    createdAt: new Date(exportModel.createdAt).toISOString(),
                    lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по постам', async () => {
            const post1 = await factory.createPost('Post', { status: PostStatus.publish });
            const post2 = await factory.createPost('Post', { status: PostStatus.publish });
            const post3 = await factory.createPost('Post', { status: PostStatus.publish });

            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('posts', post1);
                return model;
            });

            await factory.createExport('Export').then(async model => {
                await model.$add('posts', post2);
                return model;
            });
            await factory.createExport('Export').then(async model => {
                await model.$add('posts', post3);
                return model;
            });

            const response = await request(server)
                .get('/internal/exports')
                .query({ posts: [post1.urlPart] });

            expect(response.body).toBeArrayOfSize(1);

            const { posts, categories, tags, ...exportData } = response.body[0];

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(categories).toEqual([]);
            expect(tags).toEqual([]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post1.urlPart);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по тегам', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false });
            const tag2 = await factory.createTag('Tag', { isArchived: false });

            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag1);
                return model;
            });

            await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag2);
                return model;
            });
            await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag2);
                return model;
            });

            const response = await request(server)
                .get('/internal/exports')
                .query({ tags: [tag1.urlPart] });

            expect(response.body).toBeArrayOfSize(1);

            const { posts, categories, tags, ...exportData } = response.body[0];

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(posts).toEqual([]);
            expect(categories).toEqual([]);
            expect(tags).toEqual([
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
                    metaTitle: tag1.metaTitle,
                    metaDescription: tag1.metaDescription,
                    mmm: tag1.mmm,
                    createdAt: new Date(tag1.createdAt).toISOString(),
                    lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    partnershipLink: tag1.partnershipLink,
                    partnershipName: tag1.partnershipName,
                    partnershipBadgeName: tag1.partnershipBadgeName,
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по категориям', async () => {
            const category1 = await factory.createCategory('Category', { isArchived: false });
            const category2 = await factory.createCategory('Category', { isArchived: false });

            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('categories', category1);
                return model;
            });

            await factory.createExport('Export').then(async model => {
                await model.$add('categories', category2);
                return model;
            });
            await factory.createExport('Export').then(async model => {
                await model.$add('categories', category2);
                return model;
            });

            const response = await request(server)
                .get('/internal/exports')
                .query({ categories: [category1.urlPart] });

            expect(response.body).toBeArrayOfSize(1);

            const { posts, categories, tags, ...exportData } = response.body[0];

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(posts).toEqual([]);
            expect(tags).toEqual([]);
            expect(categories).toEqual([
                {
                    ...category1.toJSON(),
                    createdAt: new Date(category1.createdAt).toISOString(),
                    lastEditedAt: new Date(category1.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может сортировать экспорты по возрастанию типа, порядка и убыванию указанного поля', async () => {
            const exports = await factory
                .createManyExports('Export', 5, [
                    { type: ExportType.desktop, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 0, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 1, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    {
                        type: ExportType.morda,
                        order: 0,
                        createdAt: new Date('2021-02-20T20:10:30.000Z'),
                        section: 'Тесты',
                    },
                    {
                        type: ExportType.morda,
                        order: 0,
                        createdAt: new Date('2021-02-20T21:10:30.000Z'),
                        section: 'Обзоры',
                    },
                    {
                        type: ExportType.morda,
                        order: 0,
                        createdAt: new Date('2021-02-20T22:10:30.000Z'),
                        section: 'Новости',
                    },
                ])
                .then(models =>
                    models.map(model => ({
                        ...model.toJSON(),
                        posts: [],
                        categories: [],
                        tags: [],
                        createdAt: new Date(model.createdAt).toISOString(),
                        lastEditedAt: new Date(model.lastEditedAt).toISOString(),
                    }))
                );

            const response = await request(server).get('/internal/exports').query({ orderBySort: 'section' });

            expect(response.body).toEqual(exports);
            expect(response.statusCode).toBe(200);
        });

        it('Может сортировать экспорты по возрастанию типа, порядка и даты создания', async () => {
            const exports = await factory
                .createManyExports('Export', 5, [
                    { type: ExportType.desktop, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 0, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.mobile, order: 1, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.morda, order: 0, createdAt: new Date('2021-02-20T20:10:30.000Z') },
                    { type: ExportType.morda, order: 0, createdAt: new Date('2021-02-20T21:10:30.000Z') },
                    { type: ExportType.morda, order: 0, createdAt: new Date('2021-02-20T22:10:30.000Z') },
                ])
                .then(models =>
                    models.map(model => ({
                        ...model.toJSON(),
                        posts: [],
                        categories: [],
                        tags: [],
                        createdAt: new Date(model.createdAt).toISOString(),
                        lastEditedAt: new Date(model.lastEditedAt).toISOString(),
                    }))
                );

            const response = await request(server).get('/internal/exports').query({ orderByAsc: true });

            expect(response.body).toEqual(exports);
            expect(response.statusCode).toBe(200);
        });
    });

    describe('GET /internal/exports/:id', function () {
        it('Возвращает пустую строку и статус 200, если экспорт не найден', async () => {
            const NON_EXISTING_EXPORT_ID = 'non-existing-export-id';

            const response = await request(server).get(`/internal/exports/${NON_EXISTING_EXPORT_ID}`);

            expect(response.body).toEqual({});
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает модель экспорта с неархивными тегами/рубриками и опубликованными постами', async () => {
            const tag = await factory.createTag('Tag', { isArchived: false });
            const archivedTag = await factory.createTag('Tag', { isArchived: true });
            const category = await factory.createCategory('Category', { isArchived: false });
            const archivedCategory = await factory.createCategory('Category', { isArchived: true });
            const post = await factory.createPost('Post', { status: PostStatus.publish });
            const draftPost = await factory.createPost('Post', { status: PostStatus.draft });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', [tag, archivedTag]);
                await model.$add('categories', [category, archivedCategory]);
                await model.$add('posts', [post, draftPost]);
                return model;
            });

            const response = await request(server).get(`/internal/exports/${exportModel.id}`);

            const { posts, categories, tags, ...exportData } = response.body;

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(categories).toEqual([
                {
                    ...category.toJSON(),
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                },
            ]);
            expect(tags).toEqual([
                {
                    urlPart: tag.urlPart,
                    service: tag.service,
                    title: tag.title,
                    shortTitle: tag.shortTitle,
                    blocks: tag.blocks,
                    draftTitle: tag.draftTitle,
                    draftShortTitle: tag.draftShortTitle,
                    draftBlocks: tag.draftBlocks,
                    isArchived: tag.isArchived,
                    isHot: tag.isHot,
                    isPartnership: tag.isPartnership,
                    metaTitle: tag.metaTitle,
                    metaDescription: tag.metaDescription,
                    mmm: tag.mmm,
                    createdAt: new Date(tag.createdAt).toISOString(),
                    lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    partnershipLink: tag.partnershipLink,
                    partnershipName: tag.partnershipName,
                    partnershipBadgeName: tag.partnershipBadgeName,
                },
            ]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post.urlPart);
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает модель экспорта с пустыми тегами/рубриками/постами', async () => {
            const exportModel = await factory.createExport('Export');

            const response = await request(server).get(`/internal/exports/${exportModel.id}`);

            expect(response.body).toEqual({
                ...exportModel.toJSON(),
                posts: [],
                categories: [],
                tags: [],
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может возвращать модель экспорта с архивными тегами/рубриками и неопубликованными постами', async () => {
            const tag = await factory.createTag('Tag', { isArchived: true });
            const category = await factory.createCategory('Category', { isArchived: true });
            const post = await factory.createPost('Post', { status: PostStatus.draft });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag);
                await model.$add('categories', category);
                await model.$add('posts', post);
                return model;
            });

            const response = await request(server)
                .get(`/internal/exports/${exportModel.id}`)
                .query({ withArchived: true });

            expect(response.body).toBeObject();

            const { posts, categories, tags, ...exportData } = response.body;

            expect(exportData).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(categories).toEqual([
                {
                    ...category.toJSON(),
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                },
            ]);
            expect(tags).toEqual([
                {
                    urlPart: tag.urlPart,
                    service: tag.service,
                    title: tag.title,
                    shortTitle: tag.shortTitle,
                    blocks: tag.blocks,
                    draftTitle: tag.draftTitle,
                    draftShortTitle: tag.draftShortTitle,
                    draftBlocks: tag.draftBlocks,
                    isArchived: tag.isArchived,
                    isHot: tag.isHot,
                    isPartnership: tag.isPartnership,
                    metaTitle: tag.metaTitle,
                    metaDescription: tag.metaDescription,
                    mmm: tag.mmm,
                    createdAt: new Date(tag.createdAt).toISOString(),
                    lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    partnershipLink: tag.partnershipLink,
                    partnershipName: tag.partnershipName,
                    partnershipBadgeName: tag.partnershipBadgeName,
                },
            ]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post.urlPart);
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает модель экспорта по id с сортированными постами по order', async () => {
            const posts = await factory.createManyPosts('Post', 4, { status: PostStatus.publish });
            const exportModel = await factory.createExport('Export');

            await factory.createManyExportPosts('ExportPost', 4, [
                { post_key: posts[0]?.urlPart, export_key: exportModel.id, order: 4 },
                { post_key: posts[1]?.urlPart, export_key: exportModel.id, order: 2 },
                { post_key: posts[2]?.urlPart, export_key: exportModel.id, order: 3 },
                { post_key: posts[3]?.urlPart, export_key: exportModel.id, order: 1 },
            ]);

            const response = await request(server).get(`/internal/exports/${exportModel.id}`);

            expect(response.body.posts[0]).toMatchObject({ urlPart: posts[3]?.urlPart, export_post: { order: 1 } });
            expect(response.body.posts[3]).toMatchObject({ urlPart: posts[0]?.urlPart, export_post: { order: 4 } });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/exports', function () {
        it('Возвращает ошибку, если экспорт с таким сервисом, типом и секцией уже существует', async () => {
            const DATA = { service: Service.autoru, type: ExportType.desktop, section: 'Новости' };

            await factory.createExport('Export', DATA);

            const response = await request(server).post('/internal/exports').send(DATA);

            expect(response.body).toEqual(
                expect.objectContaining({
                    error: 'Такой экспорт уже существует',
                })
            );
            expect(response.statusCode).toBe(500);
        });

        it('Создает и возвращает модель экспорта', async () => {
            const post = await factory.createPost('Post');
            const tag = await factory.createTag('Tag');
            const category = await factory.createCategory('Category');
            const DATA = {
                service: Service.autoru,
                type: ExportType.desktop,
                section: 'Новости',
                order: 3,
                isArchived: false,
                lastEditLogin: 'operator123',
                posts: [post.urlPart],
                tags: [tag.urlPart],
                categories: [category.urlPart],
            };

            const response = await request(server).post('/internal/exports').send(DATA);

            expect(response.body).toBeObject();
            const { posts, tags, categories, ...exportData } = response.body;

            expect(exportData).toMatchObject({
                id: expect.toBeNumber(),
                service: DATA.service,
                type: DATA.type,
                section: DATA.section,
                order: DATA.order,
                isArchived: DATA.isArchived,
                lastEditLogin: DATA.lastEditLogin,
            });
            expect(new Date(exportData.createdAt)).toBeValidDate();
            expect(new Date(exportData.lastEditedAt)).toBeValidDate();
            expect(tags).toEqual([
                {
                    urlPart: tag.urlPart,
                    service: tag.service,
                    title: tag.title,
                    shortTitle: tag.shortTitle,
                    blocks: tag.blocks,
                    draftTitle: tag.draftTitle,
                    draftShortTitle: tag.draftShortTitle,
                    draftBlocks: tag.draftBlocks,
                    isArchived: tag.isArchived,
                    isHot: tag.isHot,
                    isPartnership: tag.isPartnership,
                    metaTitle: tag.metaTitle,
                    metaDescription: tag.metaDescription,
                    mmm: tag.mmm,
                    createdAt: new Date(tag.createdAt).toISOString(),
                    lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    partnershipLink: tag.partnershipLink,
                    partnershipName: tag.partnershipName,
                    partnershipBadgeName: tag.partnershipBadgeName,
                },
            ]);
            expect(categories).toEqual([
                {
                    ...category.toJSON(),
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                },
            ]);
            expect(posts).toBeArrayOfSize(1);
            expect(posts[0].urlPart).toEqual(post.urlPart);
            expect(response.statusCode).toBe(201);
        });
    });

    describe('PUT /internal/exports/:id', function () {
        it('Возвращает ошибку, если экспорт с таким сервисом, типом и секцией уже существует', async () => {
            const DATA = { service: Service.autoru, type: ExportType.desktop, section: 'Новости' };

            await factory.createExport('Export', DATA);

            const exportModel = await factory.createExport('Export', {
                service: Service.realty,
                type: ExportType.morda,
                section: 'Тесты',
            });

            const response = await request(server).put(`/internal/exports/${exportModel.id}`).send(DATA);

            expect(response.body).toMatchObject({
                error: 'Такой экспорт уже существует',
            });
            expect(response.statusCode).toBe(500);
        });

        it('Обновляет и возвращает экспорт', async () => {
            const post1 = await factory.createPost('Post', { status: PostStatus.publish });
            const post2 = await factory.createPost('Post', { status: PostStatus.publish });
            const post3 = await factory.createPost('Post', { status: PostStatus.publish });
            const tag1 = await factory.createTag('Tag', { isArchived: false });
            const tag2 = await factory.createTag('Tag', { isArchived: false });
            const tag3 = await factory.createTag('Tag', { isArchived: false });
            const category1 = await factory.createCategory('Category', { isArchived: false });
            const category2 = await factory.createCategory('Category', { isArchived: false });
            const category3 = await factory.createCategory('Category', { isArchived: false });

            const CREATE_DATA = {
                service: Service.autoru,
                type: ExportType.desktop,
                section: 'Новости',
                order: 3,
                isArchived: true,
                lastEditLogin: 'operator1',
            };
            const UPDATE_DATA = {
                service: Service.realty,
                type: ExportType.mobile,
                section: 'Статьи',
                order: 1,
                isArchived: false,
                lastEditLogin: 'operator2',
                posts: [post3.urlPart],
                tags: [tag3.urlPart],
                categories: [category3.urlPart],
            };
            const exportModel = await factory.createExport('Export', CREATE_DATA).then(async model => {
                await model.$add('tags', [tag1.urlPart, tag2.urlPart]);
                await model.$add('categories', [category1.urlPart, category2.urlPart]);
                await model.$add('posts', [post1.urlPart, post2.urlPart]);

                return model;
            });

            const response = await request(server).put(`/internal/exports/${exportModel.id}`).send(UPDATE_DATA);

            expect(response.body).toBeObject();
            const { posts: newPosts, tags: newTags, categories: newCategories, ...exportData } = response.body;

            expect(exportData).toMatchObject({
                id: exportModel.id,
                service: UPDATE_DATA.service,
                type: UPDATE_DATA.type,
                section: UPDATE_DATA.section,
                order: UPDATE_DATA.order,
                isArchived: UPDATE_DATA.isArchived,
                lastEditLogin: UPDATE_DATA.lastEditLogin,
            });
            expect(new Date(exportData.createdAt)).toBeValidDate();
            expect(new Date(exportData.lastEditedAt)).toBeValidDate();
            expect(newTags).toEqual([
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
                    metaTitle: tag3.metaTitle,
                    metaDescription: tag3.metaDescription,
                    mmm: tag3.mmm,
                    createdAt: new Date(tag3.createdAt).toISOString(),
                    lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    partnershipLink: tag3.partnershipLink,
                    partnershipName: tag3.partnershipName,
                    partnershipBadgeName: tag3.partnershipBadgeName,
                },
            ]);
            expect(newCategories).toEqual([
                {
                    ...category3.toJSON(),
                    createdAt: new Date(category3.createdAt).toISOString(),
                    lastEditedAt: new Date(category3.lastEditedAt).toISOString(),
                },
            ]);
            expect(newPosts).toBeArrayOfSize(1);
            expect(newPosts[0].urlPart).toEqual(post3.urlPart);
            expect(response.statusCode).toBe(200);
        });
    });

    describe('DELETE /internal/exports/:id', function () {
        it('Возвращает ошибку и статус 404, если экспорт не найден', async () => {
            const NON_EXISTING_EXPORT_ID = 'non-existing-export-id';

            const response = await request(server).delete(`/internal/exports/${NON_EXISTING_EXPORT_ID}`);

            expect(response.body).toMatchObject({
                error: 'Модель экспорта не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Удаляет модель экспорта', async () => {
            const post = await factory.createPost('Post');
            const tag = await factory.createTag('Tag');
            const category = await factory.createCategory('Category');
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tags', tag);
                await model.$add('categories', category);
                await model.$add('posts', post);
                return model;
            });

            const response = await request(server).delete(`/internal/exports/${exportModel.id}`);

            expect(response.body).toEqual({
                ...exportModel.toJSON(),
                createdAt: new Date(exportModel.createdAt).toISOString(),
                lastEditedAt: new Date(exportModel.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });
    });
});
