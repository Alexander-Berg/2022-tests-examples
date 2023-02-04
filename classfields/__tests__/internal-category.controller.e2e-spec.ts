import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';
import { omit } from 'lodash';

import { FactoryService } from '../../../services/factory.service';
import { Service } from '../../../types/common';
import { Category } from '../category.model';
import { createTestingApp } from '../../../tests/app';

import {
    CATEGORY_DATA_1,
    CREATE_CATEGORY_DATA_1,
    TAG_DATA_1,
    TAG_DATA_2,
    TAG_DATA_3,
    UPDATE_CATEGORY_DATA_1,
} from './fixtures';

describe('Internal categories controller', () => {
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

    describe('GET /internal/categories', function () {
        it('Возвращает только неархивные категории', async () => {
            const category1 = await factory.createCategory('Category', { isArchived: false });
            const category2 = await factory.createCategory('Category', { isArchived: false });

            await factory.createCategory('Category', { isArchived: true });

            const response = await request(server).get('/internal/categories');

            expect(response.statusCode).toBe(200);
            expect(response.body).toIncludeSameMembers([
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
            ]);
        });

        it('Может возвращать архивные и неархивные категории', async () => {
            const category1 = await factory.createCategory('Category', { isArchived: true });
            const category2 = await factory.createCategory('Category', { isArchived: false });
            const category3 = await factory.createCategory('Category', { isArchived: false });

            const response = await request(server).get('/internal/categories').query({ withArchived: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toIncludeSameMembers([
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
                {
                    ...category3.toJSON(),
                    createdAt: new Date(category3.createdAt).toISOString(),
                    lastEditedAt: new Date(category3.lastEditedAt).toISOString(),
                },
            ]);
        });

        it('Возвращает категории по указанными тегам', async () => {
            const tag1 = await factory.createTag('Tag');
            const tag2 = await factory.createTag('Tag');
            const tag3 = await factory.createTag('Tag');

            const category1 = await factory.createCategory('Category').then(async model => {
                await model.$add('tags', [tag1]);
                return model;
            });
            const category2 = await factory.createCategory('Category').then(async model => {
                await model.$add('tags', [tag2]);
                return model;
            });
            const category3 = await factory.createCategory('Category').then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            await factory.createCategory('Category').then(async model => {
                await model.$add('tags', [tag3]);
                return model;
            });
            await factory.createCategory('Category');

            const response = await request(server)
                .get('/internal/categories')
                .query({ tags: [tag1.urlPart, tag2.urlPart] });

            expect(response.statusCode).toBe(200);
            expect(response.body).toIncludeSameMembers([
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
                {
                    ...category3.toJSON(),
                    createdAt: new Date(category3.createdAt).toISOString(),
                    lastEditedAt: new Date(category3.lastEditedAt).toISOString(),
                },
            ]);
        });

        it('Возвращает пустой массив, если категории не найдены', async () => {
            const response = await request(server).get('/internal/categories').query({ service: Service.realty });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual([]);
        });
    });

    describe('GET /internal/categories/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если категория не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';

            const response = await request(server).get(`/internal/categories/${NON_EXISTING_CATEGORY_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Рубрика не найдена',
                status: 404,
            });
        });

        it('Возвращает категорию', async () => {
            const category = await factory.createCategory('Category', CATEGORY_DATA_1);

            const response = await request(server).get(`/internal/categories/${CATEGORY_DATA_1.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...CATEGORY_DATA_1,
                tags: [],
                createdAt: new Date(category.createdAt).toISOString(),
                lastEditedAt: new Date(category.lastEditedAt).toISOString(),
            });
        });

        it('Возвращает категорию с тегами', async () => {
            const tag1 = await factory.createTag('Tag', TAG_DATA_1);
            const tag2 = await factory.createTag('Tag', TAG_DATA_2);
            const category = await factory.createCategory('Category', CATEGORY_DATA_1).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            const response = await request(server).get(`/internal/categories/${CATEGORY_DATA_1.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual(
                expect.objectContaining({
                    ...CATEGORY_DATA_1,
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: new Date(category.lastEditedAt).toISOString(),
                })
            );
            expect(response.body.tags).toIncludeSameMembers([
                {
                    ...omit(TAG_DATA_1, ['partnershipImage']),
                    createdAt: new Date(tag1.createdAt).toISOString(),
                    lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                },
                {
                    ...omit(TAG_DATA_2, ['partnershipImage']),
                    createdAt: new Date(tag2.createdAt).toISOString(),
                    lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                },
            ]);
        });
    });

    describe('POST /internal/categories', function () {
        it('Возвращает ошибку и статус 500, если не задан "urlPart"', async () => {
            const DATA = { ...CREATE_CATEGORY_DATA_1, urlPart: undefined };

            const response = await request(server).post('/internal/categories').send(DATA);

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: 'Необходимо указать URL',
                status: 500,
            });
        });

        it('Возвращает ошибку и статус 500, если не задан сервис', async () => {
            const DATA = { ...CREATE_CATEGORY_DATA_1, service: undefined };

            const response = await request(server).post('/internal/categories').send(DATA);

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: 'Необходимо указать сервис',
                status: 500,
            });
        });

        it('Возвращает ошибку и статус 500, если не задан черновик заголовка', async () => {
            const DATA = { ...CREATE_CATEGORY_DATA_1, draftTitle: undefined };

            const response = await request(server).post('/internal/categories').send(DATA);

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: 'Необходимо указать заголовок',
                status: 500,
            });
        });

        it('Возвращает ошибку и статус 500, если категория с таким же "urlPart" уже существует', async () => {
            const DATA = { ...CREATE_CATEGORY_DATA_1 };

            await factory.createCategory('Category', DATA);

            const response = await request(server).post('/internal/categories').send(DATA);

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: `Рубрика с URL "${DATA.urlPart}" уже существует`,
                status: 500,
            });
        });

        it('Создает и возвращает новую категорию', async () => {
            const DATA = { ...CREATE_CATEGORY_DATA_1, userLogin: 'user123' };

            const response = await request(server).post('/internal/categories').send(DATA);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual(
                expect.objectContaining({
                    ...CREATE_CATEGORY_DATA_1,
                    createdAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                    lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                })
            );
        });
    });

    describe('PUT /internal/categories/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если категория не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';
            const UPDATE_CATEGORY_DATA = { ...UPDATE_CATEGORY_DATA_1 };

            await factory.createCategory('Category', { urlPart: 'arenda', service: Service.realty });
            await factory.createCategory('Category', { urlPart: 'podborki', service: Service.autoru });

            const response = await request(server)
                .put(`/internal/categories/${NON_EXISTING_CATEGORY_ID}`)
                .send({
                    ...UPDATE_CATEGORY_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Рубрика не найдена',
                status: 404,
            });
        });

        it('Возвращает ошибку и статус 500, если не задан сервис', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const UPDATE_CATEGORY_DATA = { ...UPDATE_CATEGORY_DATA_1, service: undefined };

            await factory.createCategory('Category', CATEGORY_DATA);

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...UPDATE_CATEGORY_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: 'Необходимо указать сервис',
                status: 500,
            });
        });

        it('Возвращает ошибку и статус 500, если не задан черновик заголовка', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const UPDATE_CATEGORY_DATA = { ...UPDATE_CATEGORY_DATA_1, draftTitle: undefined };

            await factory.createCategory('Category', CATEGORY_DATA);

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...UPDATE_CATEGORY_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(500);
            expect(response.body).toEqual({
                error: 'Необходимо указать заголовок',
                status: 500,
            });
        });

        it('Обновляет и возвращает категорию', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const UPDATE_CATEGORY_DATA = { ...UPDATE_CATEGORY_DATA_1 };
            const category = await factory.createCategory('Category', CATEGORY_DATA);

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...UPDATE_CATEGORY_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...UPDATE_CATEGORY_DATA,
                urlPart: CATEGORY_DATA.urlPart,
                title: CATEGORY_DATA.title,
                shortTitle: CATEGORY_DATA.shortTitle,
                blocks: CATEGORY_DATA.blocks,
                createdAt: new Date(category.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });

        it('Добавляет теги и возвращает категорию с тегами', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const category = await factory.createCategory('Category', CATEGORY_DATA);
            const tag1 = await factory.createTag('Tag', TAG_DATA_1);
            const tag2 = await factory.createTag('Tag', TAG_DATA_2);

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...CATEGORY_DATA,
                    tags: [TAG_DATA_1.urlPart, TAG_DATA_2.urlPart],
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual(
                expect.objectContaining({
                    ...CATEGORY_DATA,
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                })
            );
            expect(response.body.tags).toIncludeSameMembers([
                {
                    ...omit(TAG_DATA_1, ['partnershipImage']),
                    createdAt: new Date(tag1.createdAt).toISOString(),
                    lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                },
                {
                    ...omit(TAG_DATA_2, ['partnershipImage']),
                    createdAt: new Date(tag2.createdAt).toISOString(),
                    lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                },
            ]);
        });

        it('Обновляет теги и возвращает категорию с тегами', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const tag1 = await factory.createTag('Tag', TAG_DATA_1);
            const tag2 = await factory.createTag('Tag', TAG_DATA_2);
            const tag3 = await factory.createTag('Tag', TAG_DATA_3);
            const category = await factory.createCategory('Category', CATEGORY_DATA).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...CATEGORY_DATA,
                    tags: [TAG_DATA_2.urlPart, TAG_DATA_3.urlPart],
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual(
                expect.objectContaining({
                    ...CATEGORY_DATA,
                    createdAt: new Date(category.createdAt).toISOString(),
                    lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                })
            );
            expect(response.body.tags).toIncludeSameMembers([
                {
                    ...omit(TAG_DATA_2, ['partnershipImage']),
                    createdAt: new Date(tag2.createdAt).toISOString(),
                    lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                },
                {
                    ...omit(TAG_DATA_3, ['partnershipImage']),
                    partnershipImage: undefined,
                    createdAt: new Date(tag3.createdAt).toISOString(),
                    lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                },
            ]);
        });

        it('Удаляет теги и возвращает категорию с тегами', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const tag1 = await factory.createTag('Tag');
            const tag2 = await factory.createTag('Tag');
            const category = await factory.createCategory('Category', CATEGORY_DATA).then(async model => {
                await model.$add('tags', [tag1, tag2]);
                return model;
            });

            const response = await request(server)
                .put(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({
                    ...CATEGORY_DATA,
                    tags: [],
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...CATEGORY_DATA,
                tags: [],
                createdAt: new Date(category.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });
    });

    describe('DELETE /internal/categories/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если категория не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-category-id';

            const response = await request(server).delete(`/internal/categories/${NON_EXISTING_CATEGORY_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Рубрика не найдена',
                status: 404,
            });
        });

        it('Удаляет категорию и ее связи и возвращает модель', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const category = await factory.createCategory('Category', CATEGORY_DATA);
            const tag = await factory.createTag('Tag').then(async model => {
                await model.$add('category', category);
                return model;
            });
            const post = await factory.createPost('Post').then(async model => {
                await model.$add('category', category);
                return model;
            });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('category', category);
                return model;
            });

            const response = await request(server)
                .delete(`/internal/categories/${CATEGORY_DATA.urlPart}`)
                .send({ userLogin: 'user123' });

            const dbCategory = await Category.findByPk(CATEGORY_DATA.urlPart);

            const tagCategories = await tag.$get('categories');
            const postCategories = await post.$get('categories');
            const exportCategories = await exportModel.$get('categories');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...CATEGORY_DATA,
                createdAt: new Date(category.createdAt).toISOString(),
                lastEditedAt: new Date(category.lastEditedAt).toISOString(),
            });
            expect(dbCategory).toBeFalsy();
            expect(tagCategories).toHaveLength(0);
            expect(postCategories).toHaveLength(0);
            expect(exportCategories).toHaveLength(0);
        });
    });

    describe('POST /internal/categories/:urlPart/commitDraft', function () {
        it('Возвращает ошибку и статус 404, если категория не найдена', async () => {
            const NON_EXISTING_CATEGORY_ID = 'non-existing-tag';

            const response = await request(server)
                .post(`/internal/categories/${NON_EXISTING_CATEGORY_ID}/commitDraft`)
                .send();

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Категория не найдена',
                status: 404,
            });
        });

        it('Копирует черновые поля в основные и возвращает категорию', async () => {
            const CATEGORY_DATA = { ...CATEGORY_DATA_1 };
            const category = await factory.createCategory('Category', CATEGORY_DATA);

            const response = await request(server).post(`/internal/categories/${CATEGORY_DATA.urlPart}/commitDraft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...CATEGORY_DATA,
                title: CATEGORY_DATA.draftTitle,
                shortTitle: CATEGORY_DATA.draftShortTitle,
                blocks: CATEGORY_DATA.draftBlocks,
                createdAt: new Date(category.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });
    });
});
