import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { PollStatistics } from '../../poll-statistics/poll-statistics.model';
import { createTestingApp } from '../../../tests/app';

describe('Public polls controller', () => {
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

    describe('GET /polls/:id/statistics', function () {
        it('Возвращает ошибку и статус 404, если опрос не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-poll-id';

            const response = await request(server).get(`/polls/${NON_EXISTING_POLL_ID}/statistics`);

            expect(response.body).toEqual({
                error: 'Опрос не найден',
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает отсортированную статистику', async () => {
            const POLL_DATA = {
                id: 10,
                question: 'Сколько стоит ваша квартира',
                answers: ['До 1 миллиона', 'От 1 до 3х миллионов', 'Более 3х миллионов'],
            };

            await factory.createPoll('Poll', POLL_DATA).then(async model => {
                await model.$create('statistic', {
                    answerIndex: 2,
                    count: 5,
                });
                await model.$create('statistic', {
                    answerIndex: 0,
                    count: 3,
                });
                await model.$create('statistic', {
                    answerIndex: 1,
                    count: 9,
                });

                return model;
            });

            const response = await request(server).get(`/polls/${POLL_DATA.id}/statistics`);

            expect(response.body).toEqual({
                votesCount: 17,
                answers: [
                    {
                        answerIndex: 0,
                        percentage: 17.6,
                    },
                    {
                        answerIndex: 1,
                        percentage: 52.9,
                    },
                    {
                        answerIndex: 2,
                        percentage: 29.4,
                    },
                ],
            });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /polls/:id/vote', function () {
        it('Возвращает ошибку и статус 404, если опрос не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-poll-id';

            const response = await request(server).post(`/polls/${NON_EXISTING_POLL_ID}/vote`).send({ answer: 0 });

            expect(response.body).toEqual({
                error: 'Опрос не найден',
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает ошибку и статус 400, если не выбран вариант ответа', async () => {
            const POLL_ID = 10;

            await factory.createPoll('Poll', { id: POLL_ID });

            const response = await request(server).post(`/polls/${POLL_ID}/vote`).send({});

            expect(response.body).toEqual({
                error: 'Не выбран вариант ответа',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 404, если выбран несуществующий вариант ответа', async () => {
            const POLL_DATA = {
                id: 10,
                answers: ['Один', 'Два', 'Три'],
            };
            const NON_EXISTING_POLL_ANSWER_INDEX = 200;

            await factory.createPoll('Poll', POLL_DATA).then(model => {
                const answers = model.answers || [];

                return Promise.all(
                    answers.map((_, answerIndex) => {
                        return model.$create('statistic', { answerIndex, pollId: model.id });
                    })
                );
            });

            const response = await request(server)
                .post(`/polls/${POLL_DATA.id}/vote`)
                .send({ answer: NON_EXISTING_POLL_ANSWER_INDEX });

            expect(response.body).toEqual({
                error: 'Вариант ответа не найден',
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Сохраняет голос и возвращает пустой объект', async () => {
            const POLL_DATA = {
                id: 10,
                answers: ['Один', 'Два', 'Три'],
            };
            const ANSWER_INDEX = 1;

            await factory.createPoll('Poll', POLL_DATA).then(model => {
                const answers = model.answers || [];

                return Promise.all(
                    answers.map((_, answerIndex) => {
                        return model.$create('statistic', { answerIndex, pollId: model.id });
                    })
                );
            });

            const response = await request(server).post(`/polls/${POLL_DATA.id}/vote`).send({ answer: ANSWER_INDEX });

            const dbAnswer = await PollStatistics.findOne({
                where: { pollId: POLL_DATA.id, answerIndex: ANSWER_INDEX },
            });

            expect(response.body).toEqual({});
            expect(dbAnswer?.get('count')).toBe(1);
            expect(response.statusCode).toBe(200);
        });
    });
});
