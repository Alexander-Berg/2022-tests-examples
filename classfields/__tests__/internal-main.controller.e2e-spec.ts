import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import sortBy from 'lodash/sortBy';

import { FactoryService } from '../../../services/factory.service';
import { createTestingApp } from '../../../tests/app';

import { MAIN_DATA_1, UPDATE_MAIN_DATA_1, UPDATE_MAIN_DATA_2 } from './fixtures';

describe('Internal main controller', () => {
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

    describe('GET /internal/main/:service', function () {
        it('Возвращает модель главной', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const main = await factory.createMain('Main', MAIN_DATA);

            const response = await request(server).get(`/internal/main/${MAIN_DATA.service}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...MAIN_DATA,
                tags: [],
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: new Date(main.lastEditedAt).toISOString(),
            });
        });

        it('Возвращает модель главной с тегами, сортированными по order', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const shuffledOrder = [3, 2, 4, 1];
            const tags = await factory.createManyTags('Tag', 4);
            const mainTags = await factory.createManyMainTag(
                'MainTag',
                4,
                tags.map((tag, idx) => ({ tag_key: tag.urlPart, order: shuffledOrder[idx] }))
            );
            const main = await factory.createMain('Main', MAIN_DATA);

            const response = await request(server).get(`/internal/main/${MAIN_DATA.service}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...MAIN_DATA,
                tags: sortBy(mainTags, 'order').map(mainTag => mainTag.tag_key),
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: new Date(main.lastEditedAt).toISOString(),
            });
        });
    });

    describe('PUT /internal/main/:service', function () {
        it('Обновляет и возвращает модель главной', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const UPDATE_MAIN_DATA = { ...UPDATE_MAIN_DATA_1 };
            const main = await factory.createMain('Main', MAIN_DATA);

            const response = await request(server)
                .put(`/internal/main/${MAIN_DATA.service}`)
                .send({
                    ...UPDATE_MAIN_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                service: MAIN_DATA.service,
                blocks: MAIN_DATA.blocks,
                draftBlocks: UPDATE_MAIN_DATA.draftBlocks,
                tags: [],
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });

        it('Фильтрует при обновлении блоки в секциях на null, undefined, false', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const UPDATE_MAIN_DATA = { ...UPDATE_MAIN_DATA_2 };
            const main = await factory.createMain('Main', MAIN_DATA);

            const response = await request(server)
                .put(`/internal/main/${MAIN_DATA.service}`)
                .send({
                    ...UPDATE_MAIN_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(200);

            expect(response.body).toEqual({
                service: MAIN_DATA.service,
                blocks: MAIN_DATA.blocks,
                draftBlocks: [
                    {
                        type: 'section',
                        section: {
                            blocks: [
                                {
                                    type: 'threeRight',
                                    threeRight: ['v-kazahstane-prodayut-betmobil-po-cene-desyati-horoshih-mersedesov'],
                                },
                            ],
                        },
                    },
                ],
                tags: [],
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });

        it('Обновляет модель главной, сохраняя новые теги', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const main = await factory.createMain('Main', MAIN_DATA);
            const tags = await factory.createManyTags('Tag', 3);
            const tagsIds = tags.map(tag => tag.urlPart);

            const response = await request(server).put(`/internal/main/${MAIN_DATA.service}`).send({
                tags: tagsIds,
                userLogin: 'user123',
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                service: MAIN_DATA.service,
                blocks: MAIN_DATA.blocks,
                draftBlocks: MAIN_DATA.draftBlocks,
                tags: tagsIds,
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });
    });

    describe('POST /internal/main/:service/commitDraft', function () {
        it('Возвращает ошибку и статус 404, если модель главной не найдена', async () => {
            const NON_EXISTING_SERVICE = 'unknown';

            const response = await request(server).post(`/internal/main/${NON_EXISTING_SERVICE}/commitDraft`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Главная не найдена',
                status: 404,
            });
        });

        it('Копирует черновые поля в основные и возвращает модель главной', async () => {
            const MAIN_DATA = { ...MAIN_DATA_1 };
            const main = await factory.createMain('Main', MAIN_DATA);

            const response = await request(server).post(`/internal/main/${MAIN_DATA.service}/commitDraft`);

            expect(response.body).toEqual({
                service: MAIN_DATA.service,
                blocks: MAIN_DATA.draftBlocks,
                draftBlocks: MAIN_DATA.draftBlocks,
                tags: [],
                createdAt: new Date(main.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
            expect(response.statusCode).toBe(200);
        });
    });
});
