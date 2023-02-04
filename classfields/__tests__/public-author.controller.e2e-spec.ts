import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { Author as AuthorModel } from '../author.model';
import { AuthorSocialNetwork as AuthorSocialNetworkModel } from '../../author-social-network/author-social-network.model';

import { fixtures } from './public-author.controller.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Public authors controller', () => {
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

    describe('GET /authors/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если автор не найден', async () => {
            const NON_EXISTING_POLL_ID = 'non-existing-author-id';

            const response = await request(server).get(`/authors/${NON_EXISTING_POLL_ID}`);

            expect(response.body).toEqual({
                error: 'Автор не найден',
                status: 404,
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает автора по его urlPart', async () => {
            const { AUTHOR_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const response = await request(server).get(`/authors/${author.urlPart}`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });
});
