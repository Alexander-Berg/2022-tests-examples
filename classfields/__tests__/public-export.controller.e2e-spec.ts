import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { ExportType } from '../../../types/export';
import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { PostStatus } from '../../../types/post';
import { Post } from '../../../modules/post/post.model';

import { fixtures } from './public-export.controller.fixtures';

const DATE_NOW = '2022-03-29T10:32:00.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

jest.mock('internal-core/server/resources/redis-mag', () => {
    return {
        getInfiniteMordaExport: () => Promise.resolve(fixtures.infiniteMordaCache),
    };
});

describe('Public exports controller', () => {
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

    describe('GET /exports/desktop', function () {
        it('Возвращает 404 ошибку, если десктопный экспорт не создан', async () => {
            const response = await request(server).get('/exports/desktop');

            expect(response.body.error).toEqual('Экспорт не найден');
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает модель десктопного экспорта', async () => {
            const {
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            const exportPosts = await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const category1 = await factory
                .createCategory('Category', CATEGORY_ATTRIBUTES_1)
                .then(async categoryModel => {
                    await categoryModel.$set('posts', [exportPosts[0] as Post]);

                    return categoryModel;
                });
            const category2 = await factory
                .createCategory('Category', CATEGORY_ATTRIBUTES_2)
                .then(async categoryModel => {
                    await categoryModel.$set('posts', [exportPosts[1] as Post, exportPosts[2] as Post]);

                    return categoryModel;
                });

            const exportModel = await factory.createExport('Export', { type: ExportType.desktop }).then(model => {
                model.$set('categories', [category1, category2]);

                return model;
            });

            await factory.createManyExportPosts('ExportPost', 2, [
                { post_key: exportPosts[4]!.urlPart, export_key: exportModel.id, order: 1 },
                { post_key: exportPosts[3]!.urlPart, export_key: exportModel.id, order: 2 },
            ]);

            await factory.createManyPosts('Post', 4, [
                { status: PostStatus.publish, publishAt: '2022-03-30T01:00:00.000Z' },
                { status: PostStatus.publish, publishAt: '2022-03-30T01:00:00.000Z' },
                { status: PostStatus.draft },
                { status: PostStatus.draft },
            ]);

            const response = await request(server).get('/exports/desktop');

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('GET /exports/infiniteMorda', function () {
        it('Возвращает кешированный экспорт при достаточном количестве постов в нём', async () => {
            const response = await request(server).get('/exports/infiniteMorda').query({ pageNumber: 0, pageSize: 3 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает кешированный экспорт, дополненный свежими постами при недостаточном количестве постов в нём', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5 } =
                getFixtures(fixtures);

            await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const response = await request(server).get('/exports/infiniteMorda').query({ pageNumber: 0, pageSize: 5 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает свежие посты при отсутствии данных в кеше', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5 } =
                getFixtures(fixtures);

            await factory.createManyPosts('Post', 5, [
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const response = await request(server).get('/exports/infiniteMorda').query({ pageNumber: 1, pageSize: 3 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });
});
