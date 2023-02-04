import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { Author as AuthorModel } from '../author.model';
import { AuthorSocialNetwork as AuthorSocialNetworkModel } from '../../author-social-network/author-social-network.model';

import { fixtures } from './internal-author.controller.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Internal authors controller', () => {
    let app: INestApplication;
    let server;

    beforeEach(async () => {
        const testingApp = await createTestingApp();

        app = testingApp.app;
        await app.init();

        server = app.getHttpServer();
    });

    afterEach(async () => {
        await app.close();
    });

    describe('GET /internal/authors', function () {
        it('Возвращает всех авторов', async () => {
            const { AUTHOR_DATA_1, AUTHOR_DATA_2, AUTHOR_DATA_3 } = getFixtures(fixtures);

            for (const authorData of [AUTHOR_DATA_1, AUTHOR_DATA_2, AUTHOR_DATA_3]) {
                // eslint-disable-next-line no-await-in-loop
                await AuthorModel.create(authorData, { include: AuthorSocialNetworkModel });
            }

            const response = await request(server).get('/internal/authors');

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('GET /internal/authors/:id', function () {
        it('Возвращает ошибку и статус 404, если автор не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-author-id';

            const response = await request(server).get(`/internal/authors/${NON_EXISTING_POLL_ID}`);

            expect(response.body).toEqual({
                error: 'Автор не найден',
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает автора', async () => {
            const { AUTHOR_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server).get(`/internal/authors/${author.id}`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/authors', function () {
        it('Возвращает ошибку и статус 400, если не указано имя', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const response = await request(server)
                .post('/internal/authors')
                .send({ ...AUTHOR_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать имя',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указан urlPart', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const response = await request(server)
                .post('/internal/authors')
                .send({ ...AUTHOR_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать URL',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указана должность', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const response = await request(server)
                .post('/internal/authors')
                .send({ ...AUTHOR_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать должность',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указан логин пользователя', async () => {
            const { AUTHOR_DATA } = getFixtures(fixtures);

            const response = await request(server).post('/internal/authors').send(AUTHOR_DATA);

            expect(response.body).toEqual({
                error: 'Необходимо указать логин пользователя',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если уже существует пользователь с таким urlPart', async () => {
            const { AUTHOR_DATA_1, AUTHOR_DATA_2, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA_1);

            const response = await request(server)
                .post('/internal/authors')
                .send({ ...AUTHOR_DATA_2, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: `Автор с URL "${author.urlPart}" уже существует`,
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Создает и возвращает нового автора', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const response = await request(server)
                .post('/internal/authors')
                .send({ ...AUTHOR_DATA, userLogin: USER_LOGIN });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('PUT /internal/authors/:id', function () {
        it('Возвращает ошибку и статус 400, если не указано имя', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .put(`/internal/authors/${author.id}`)
                .send({ ...AUTHOR_UPDATE_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать имя',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указан urlPart', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .put(`/internal/authors/${author.id}`)
                .send({ ...AUTHOR_UPDATE_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать URL',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указана должность', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .put(`/internal/authors/${author.id}`)
                .send({ ...AUTHOR_UPDATE_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: 'Необходимо указать должность',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если не указан логин пользователя', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server).put(`/internal/authors/${author.id}`).send(AUTHOR_UPDATE_DATA);

            expect(response.body).toEqual({
                error: 'Необходимо указать логин пользователя',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 400, если уже существует пользователь с таким urlPart', async () => {
            const { AUTHOR_DATA_1, AUTHOR_DATA_2, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA_1, { include: AuthorSocialNetworkModel });
            const author2 = await AuthorModel.create(AUTHOR_DATA_2, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .put(`/internal/authors/${author.id}`)
                .send({ ...AUTHOR_UPDATE_DATA, userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: `Автор с URL "${author2.urlPart}" уже существует`,
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Обновляет и возвращает автора', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .put(`/internal/authors/${author.id}`)
                .send({ ...AUTHOR_UPDATE_DATA, userLogin: USER_LOGIN });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('DELETE /internal/authors/:id', function () {
        it('Возвращает ошибку и статус 400, если не указан логин пользователя', async () => {
            const { AUTHOR_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server).delete(`/internal/authors/${author.id}`);

            expect(response.body).toEqual({
                error: 'Необходимо указать логин пользователя',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });

        it('Возвращает ошибку и статус 404, если автор не найден', async () => {
            const NON_EXISTING_AUTHOR_ID = 123;
            const USER_LOGIN = 'editor-1';

            const response = await request(server)
                .delete(`/internal/authors/${NON_EXISTING_AUTHOR_ID}`)
                .send({ userLogin: USER_LOGIN });

            expect(response.body).toEqual({
                error: `Автор не найден`,
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Удаляет и возвращает удаленного автора', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server)
                .delete(`/internal/authors/${author.id}`)
                .send({ userLogin: USER_LOGIN });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });
});
