import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { createTestingApp } from '../../../tests/app';

import {
    POLL_DATA_1,
    POLL_DATA_2,
    POLL_DATA_3,
    POLL_DATA_4,
    POLL_DATA_5,
    CREATE_POLL_DATA_1,
    UPDATE_POLL_DATA_1,
} from './fixtures';

describe('Internal polls controller', () => {
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

    describe('GET /internal/polls', function () {
        it('Возвращает все опросы', async () => {
            const POLL_1 = await factory.createPoll('Poll', POLL_DATA_1);
            const POLL_2 = await factory.createPoll('Poll', POLL_DATA_2);
            const POLL_3 = await factory.createPoll('Poll', POLL_DATA_3);

            const response = await request(server).get('/internal/polls');

            expect(response.body).toIncludeSameMembers([
                {
                    ...POLL_DATA_1,
                    createdAt: new Date(POLL_1.createdAt).toISOString(),
                    lastEditedAt: new Date(POLL_1.lastEditedAt).toISOString(),
                },
                {
                    ...POLL_DATA_2,
                    createdAt: new Date(POLL_2.createdAt).toISOString(),
                    lastEditedAt: new Date(POLL_2.lastEditedAt).toISOString(),
                },
                {
                    ...POLL_DATA_3,
                    createdAt: new Date(POLL_3.createdAt).toISOString(),
                    lastEditedAt: new Date(POLL_3.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Может фильтровать по тексту вопроса', async () => {
            const QUESTION = 'сколько';

            await factory.createPoll('Poll', POLL_DATA_1);
            const POLL_1 = await factory.createPoll('Poll', POLL_DATA_4);
            const POLL_2 = await factory.createPoll('Poll', POLL_DATA_5);

            const response = await request(server).get('/internal/polls').query({ question: QUESTION });

            expect(response.body).toIncludeSameMembers([
                {
                    ...POLL_DATA_4,
                    createdAt: new Date(POLL_1.createdAt).toISOString(),
                    lastEditedAt: new Date(POLL_1.lastEditedAt).toISOString(),
                },
                {
                    ...POLL_DATA_5,
                    createdAt: new Date(POLL_2.createdAt).toISOString(),
                    lastEditedAt: new Date(POLL_2.lastEditedAt).toISOString(),
                },
            ]);
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает пустой массив, если опросы не найдены', async () => {
            const BAD_QUESTION = 'тратата';
            const QUESTION_1 = 'сколько';
            const QUESTION_2 = 'за что';

            await factory.createPoll('Poll', { question: QUESTION_1 });
            await factory.createPoll('Poll', { question: QUESTION_2 });

            const response = await request(server).get('/internal/polls').query({ question: BAD_QUESTION });

            expect(response.body).toEqual([]);
            expect(response.statusCode).toBe(200);
        });
    });

    describe('GET /internal/polls/:id', function () {
        it('Возвращает ошибку и статус 404, если опрос не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-poll-id';

            const response = await request(server).get(`/internal/polls/${NON_EXISTING_POLL_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Опрос не найден',
                status: 404,
            });
        });

        it('Возвращает опрос', async () => {
            const POLL_DATA = { ...POLL_DATA_1 };
            const POLL_1 = await factory.createPoll('Poll', POLL_DATA);

            const response = await request(server).get(`/internal/polls/${POLL_DATA.id}`);

            expect(response.body).toEqual({
                ...POLL_DATA,
                createdAt: new Date(POLL_1.createdAt).toISOString(),
                lastEditedAt: new Date(POLL_1.lastEditedAt).toISOString(),
            });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/polls', function () {
        it('Создает и возвращает новый опрос', async () => {
            const POLL_DATA = { ...CREATE_POLL_DATA_1 };

            const response = await request(server).post('/internal/polls').send(POLL_DATA);

            expect(response.body).toEqual(
                expect.objectContaining({
                    ...POLL_DATA,
                    id: expect.toBeNumber(),
                    createdAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                    lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
                })
            );
            expect(response.statusCode).toBe(200);
        });
    });

    describe('PUT /internal/polls/:id', function () {
        it('Возвращает ошибку и статус 404, если опрос не найден', async () => {
            const NON_EXISTING_POLL_ID = 1000;
            const POLL_DATA = { ...CREATE_POLL_DATA_1 };

            const response = await request(server).put(`/internal/polls/${NON_EXISTING_POLL_ID}`).send(POLL_DATA);

            expect(response.body).toEqual({
                error: 'При обновлении опроса произошла ошибка',
                status: 500,
            });
            expect(response.statusCode).toBe(500);
        });

        it('Обновляет и возвращает опрос', async () => {
            const POLL_DATA = { ...POLL_DATA_1 };
            const UPDATE_POLL_DATA = { ...UPDATE_POLL_DATA_1 };
            const POLL = await factory.createPoll('Poll', POLL_DATA);

            const response = await request(server).put(`/internal/polls/${POLL_DATA.id}`).send(UPDATE_POLL_DATA);

            expect(response.body).toEqual({
                id: POLL_DATA.id,
                service: POLL_DATA.service,
                question: UPDATE_POLL_DATA.question,
                answers: UPDATE_POLL_DATA.answers,
                createdAt: new Date(POLL.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает ошибку если вопрос не заполнен', async () => {
            const POLL_DATA = { ...POLL_DATA_1 };
            const UPDATE_POLL_DATA = { ...UPDATE_POLL_DATA_1, question: '' };

            const response = await request(server).put(`/internal/polls/${POLL_DATA.id}`).send(UPDATE_POLL_DATA);

            expect(response.body).toEqual({
                error: 'Необходимо указать вопрос',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });
    });

    describe('GET /internal/polls/:id/statistics', function () {
        it('Возвращает ошибку и статус 404, если опрос не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-poll-id';

            const response = await request(server).get(`/internal/polls/${NON_EXISTING_POLL_ID}/statistics`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                error: 'Опрос не найден',
                status: 404,
            });
        });

        it('Возвращает статистику', async () => {
            const POLL_DATA = {
                id: 10,
                question: 'Сколько стоит ваша квартира',
                answers: ['До 1 миллиона', 'От 1 до 3х миллионов', 'Более 3х миллионов'],
            };

            await factory.createPoll('Poll', POLL_DATA).then(async model => {
                await model.$create('statistic', {
                    answerIndex: 0,
                    count: 3,
                });
                await model.$create('statistic', {
                    answerIndex: 1,
                    count: 9,
                });
                await model.$create('statistic', {
                    answerIndex: 2,
                    count: 5,
                });

                return model;
            });

            const response = await request(server).get(`/internal/polls/${POLL_DATA.id}/statistics`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                votesCount: 17,
                answers: [
                    {
                        answer: 'До 1 миллиона',
                        answerIndex: 0,
                        count: 3,
                        percentage: 17.6,
                    },
                    {
                        answer: 'От 1 до 3х миллионов',
                        answerIndex: 1,
                        count: 9,
                        percentage: 52.9,
                    },
                    {
                        answer: 'Более 3х миллионов',
                        answerIndex: 2,
                        count: 5,
                        percentage: 29.4,
                    },
                ],
            });
        });
    });
});
