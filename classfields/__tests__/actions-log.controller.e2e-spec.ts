import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { AppModule } from '../../app/app.module';
import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';

describe('Internal actions log controller', () => {
    let app: INestApplication;
    let Factory: FactoryService;
    let server;

    beforeEach(async () => {
        const moduleFixture: TestingModule = await Test.createTestingModule({
            imports: [AppModule],
            providers: [FactoryService],
        }).compile();

        Factory = await moduleFixture.resolve(FactoryService);
        app = await moduleFixture.createNestApplication();
        await app.init();
        server = app.getHttpServer();
    });

    afterEach(async () => {
        await app.close();
    });

    describe('GET /logs', function () {
        const DEFAULT_LIMIT = 25;

        it('Возвращает последние 25 логов', async () => {
            await Factory.createManyActionsLogs('ActionsLog', 5, { timestamp: '2021-01-19T10:30:30.000Z' });
            const logs = await Factory.createManyActionsLogs('ActionsLog', DEFAULT_LIMIT, [
                { timestamp: '2021-01-20T10:30:30.000Z' },
                { timestamp: '2021-01-20T10:29:30.000Z' },
                { timestamp: '2021-01-20T10:28:30.000Z' },
                { timestamp: '2021-01-20T10:27:30.000Z' },
                { timestamp: '2021-01-20T10:26:30.000Z' },
                { timestamp: '2021-01-20T10:25:30.000Z' },
                { timestamp: '2021-01-20T10:24:30.000Z' },
                { timestamp: '2021-01-20T10:23:30.000Z' },
                { timestamp: '2021-01-20T10:22:30.000Z' },
                { timestamp: '2021-01-20T10:21:30.000Z' },
                { timestamp: '2021-01-20T10:20:30.000Z' },
                { timestamp: '2021-01-20T10:19:30.000Z' },
                { timestamp: '2021-01-20T10:18:30.000Z' },
                { timestamp: '2021-01-20T10:17:30.000Z' },
                { timestamp: '2021-01-20T10:16:30.000Z' },
                { timestamp: '2021-01-20T10:15:30.000Z' },
                { timestamp: '2021-01-20T10:14:30.000Z' },
                { timestamp: '2021-01-20T10:13:30.000Z' },
                { timestamp: '2021-01-20T10:12:30.000Z' },
                { timestamp: '2021-01-20T10:11:30.000Z' },
                { timestamp: '2021-01-20T10:10:30.000Z' },
                { timestamp: '2021-01-20T10:09:30.000Z' },
                { timestamp: '2021-01-20T10:08:30.000Z' },
                { timestamp: '2021-01-20T10:07:30.000Z' },
                { timestamp: '2021-01-20T10:06:30.000Z' },
            ]).then(models =>
                models.map(model => ({
                    ...model.toJSON(),
                    timestamp: new Date(model.timestamp).toISOString(),
                }))
            );

            const response = await request(server).get('/logs');

            expect(response.body.data).toHaveLength(DEFAULT_LIMIT);
            expect(response.body.total).toBe(30);
            expect(logs).toEqual(response.body.data);
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает логи с указанным лимитом', async () => {
            const LIMIT = 3;
            const log1 = await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            const log2 = await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            const log3 = await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T18:10:30.000Z' });

            await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T17:10:30.000Z' });

            const response = await request(server).get('/logs').query({ pageSize: LIMIT });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...log1.toJSON(),
                        timestamp: new Date(log1.timestamp).toISOString(),
                    },
                    {
                        ...log2.toJSON(),
                        timestamp: new Date(log2.timestamp).toISOString(),
                    },
                    {
                        ...log3.toJSON(),
                        timestamp: new Date(log3.timestamp).toISOString(),
                    },
                ],
                total: 4,
            });
        });

        it('Возвращает логи с указанным: лимитом и страницей', async () => {
            const LIMIT = 3;
            const PAGE_NUMBER = 1;

            await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T18:10:30.000Z' });

            const log = await Factory.createActionsLog('ActionsLog', { timestamp: '2021-01-20T17:10:30.000Z' });

            const response = await request(server).get('/logs').query({
                pageSize: LIMIT,
                pageNum: PAGE_NUMBER,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...log.toJSON(),
                        timestamp: new Date(log.timestamp).toISOString(),
                    },
                ],
                total: 4,
            });
        });

        it('Может фильтровать по автору', async () => {
            const AUTHOR_1 = 'user1';
            const AUTHOR_2 = 'user2';
            const TOTAL_LOGS = 5;

            const logs = await Factory.createManyActionsLogs('ActionsLog', TOTAL_LOGS, { author: AUTHOR_1 }).then(
                models =>
                    models.map(model => ({
                        ...model.toJSON(),
                        timestamp: new Date(model.timestamp).toISOString(),
                    }))
            );

            await Factory.createManyActionsLogs('ActionsLog', 2, { author: AUTHOR_2 });

            const response = await request(server).get('/logs').query({ author: AUTHOR_1, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body.data).toIncludeSameMembers(logs);
            expect(response.body.total).toBe(TOTAL_LOGS);
        });

        it('Может фильтровать по действию', async () => {
            const TOTAL_LOGS = 5;

            const logs = await Factory.createManyActionsLogs('ActionsLog', TOTAL_LOGS, {
                action: ActionLogAction.create,
            }).then(models =>
                models.map(model => ({
                    ...model.toJSON(),
                    timestamp: new Date(model.timestamp).toISOString(),
                }))
            );

            await Factory.createManyActionsLogs('ActionsLog', 2, { action: ActionLogAction.update });

            const response = await request(server).get('/logs').query({ action: ActionLogAction.create, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body.data).toIncludeSameMembers(logs);
            expect(response.body.total).toBe(TOTAL_LOGS);
        });

        it('Может фильтровать по сущности', async () => {
            const TOTAL_LOGS = 5;
            const logs = await Factory.createManyActionsLogs('ActionsLog', TOTAL_LOGS, {
                entity: ActionLogEntity.post,
            }).then(models =>
                models.map(model => ({
                    ...model.toJSON(),
                    timestamp: new Date(model.timestamp).toISOString(),
                }))
            );

            await Factory.createManyActionsLogs('ActionsLog', 2, { entity: ActionLogEntity.tag });

            const response = await request(server).get('/logs').query({ entity: ActionLogEntity.post, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body.data).toIncludeSameMembers(logs);
            expect(response.body.total).toBe(TOTAL_LOGS);
        });

        it('Может фильтровать по "urlPart"', async () => {
            await Factory.createManyActionsLogs('ActionsLog', 5);
            const URL_PART = 'ipoteka';
            const log = await Factory.createActionsLog('ActionsLog', { urlPart: URL_PART });

            const response = await request(server).get('/logs').query({ urlPart: URL_PART, pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        ...log.toJSON(),
                        timestamp: new Date(log.timestamp).toISOString(),
                    },
                ],
                total: 1,
            });
        });

        it('Может фильтровать по нижней дате создания', async () => {
            await Factory.createManyActionsLogs('ActionsLog', 2, { timestamp: '2021-01-10T20:10:30.000Z' });
            const TOTAL_LOGS = 3;
            const logs = await Factory.createManyActionsLogs('ActionsLog', TOTAL_LOGS, {
                timestamp: '2021-01-25T20:10:30.000Z',
            }).then(models =>
                models.map(model => ({
                    ...model.toJSON(),
                    timestamp: new Date(model.timestamp).toISOString(),
                }))
            );

            const response = await request(server)
                .get('/logs')
                .query({ createDateFrom: '2021-01-20T20:10:30.000Z', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body.data).toIncludeSameMembers(logs);
            expect(response.body.total).toBe(TOTAL_LOGS);
        });

        it('Может фильтровать по верхней дате создания', async () => {
            await Factory.createManyActionsLogs('ActionsLog', 2, { timestamp: '2021-01-25T20:10:30.000Z' });
            const TOTAL_LOGS = 3;
            const logs = await Factory.createManyActionsLogs('ActionsLog', TOTAL_LOGS, {
                timestamp: '2021-01-10T20:10:30.000Z',
            }).then(models =>
                models.map(model => ({
                    ...model.toJSON(),
                    timestamp: new Date(model.timestamp).toISOString(),
                }))
            );

            const response = await request(server)
                .get('/logs')
                .query({ createDateTo: '2021-01-20T20:10:30.000Z', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body.data).toIncludeSameMembers(logs);
            expect(response.body.total).toBe(TOTAL_LOGS);
        });
    });
});
