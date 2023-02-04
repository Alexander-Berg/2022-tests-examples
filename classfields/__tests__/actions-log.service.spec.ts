import { Test, TestingModule } from '@nestjs/testing';
import 'jest-extended';

import { FactoryService } from '../../../services/factory.service';
import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';
import { ActionsLog as ActionsLogModel } from '../../actions-log/actions-log.model';
import { ActionsLogService } from '../actions-log.service';
import { ActionsLogModule } from '../actions-log.module';
import { getFixtures } from '../../../tests/get-fixtures';

import { fixtures } from './actions-log.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Actions log service', () => {
    let testingModule: TestingModule;
    let factoryService: FactoryService;
    let actionsLogService: ActionsLogService;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [ActionsLogModule],
            providers: [FactoryService],
        }).compile();

        factoryService = await testingModule.resolve(FactoryService);
        actionsLogService = await testingModule.resolve(ActionsLogService);
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('findAndCountAll', function () {
        const DEFAULT_PAGE_SIZE = 25;

        it('Возвращает последние 25 моделей логов', async () => {
            await factoryService.createManyActionsLogs('ActionsLog', 5, { timestamp: '2021-01-19T10:30:30.000Z' });
            const LOGS = await factoryService
                .createManyActionsLogs('ActionsLog', DEFAULT_PAGE_SIZE, [
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
                ])
                .then(models => models.map(model => model.toJSON()));

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll();

            expect(ACTUAL_LOGS.rows).toHaveLength(DEFAULT_PAGE_SIZE);
            expect(ACTUAL_LOGS.count).toBe(30);
            ACTUAL_LOGS.rows.map(log => {
                expect(log).toBeInstanceOf(ActionsLogModel);
                expect(LOGS).toContainEqual(log.toJSON());
            });
        });

        it('Возвращает модели логов с указанным лимитом', async () => {
            const PAGE_SIZE = 3;
            const LOG_1 = await factoryService.createActionsLog('ActionsLog', {
                timestamp: '2021-01-20T20:10:30.000Z',
            });
            const LOG_2 = await factoryService.createActionsLog('ActionsLog', {
                timestamp: '2021-01-20T20:10:30.000Z',
            });
            const LOG_3 = await factoryService.createActionsLog('ActionsLog', {
                timestamp: '2021-01-20T18:10:30.000Z',
            });

            await factoryService.createActionsLog('ActionsLog', { timestamp: '2021-01-20T17:10:30.000Z' });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({ pageSize: PAGE_SIZE });

            expect(ACTUAL_LOGS).toEqual({
                rows: [LOG_1, LOG_2, LOG_3],
                count: 4,
            });
        });

        it('Возвращает модели логов с указанными лимитом и страницей', async () => {
            const PAGE_SIZE = 3;
            const PAGE_NUMBER = 1;

            await factoryService.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            await factoryService.createActionsLog('ActionsLog', { timestamp: '2021-01-20T20:10:30.000Z' });
            await factoryService.createActionsLog('ActionsLog', { timestamp: '2021-01-20T18:10:30.000Z' });

            const LOG = await factoryService.createActionsLog('ActionsLog', { timestamp: '2021-01-20T17:10:30.000Z' });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                pageSize: PAGE_SIZE,
                pageNumber: PAGE_NUMBER,
            });

            expect(ACTUAL_LOGS).toEqual({
                rows: [LOG],
                count: 4,
            });
        });

        it('Может фильтровать по автору', async () => {
            const AUTHOR_1 = 'user1';
            const AUTHOR_2 = 'user2';
            const LOGS_COUNT = 5;

            const LOGS = await factoryService.createManyActionsLogs('ActionsLog', LOGS_COUNT, { author: AUTHOR_1 });

            await factoryService.createManyActionsLogs('ActionsLog', 2, { author: AUTHOR_2 });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                author: AUTHOR_1,
                pageSize: 10,
            });

            expect(ACTUAL_LOGS.rows).toIncludeSameMembers(LOGS);
            expect(ACTUAL_LOGS.count).toBe(LOGS_COUNT);
        });

        it('Может фильтровать по действию', async () => {
            const LOGS_COUNT = 5;

            const LOGS = await factoryService.createManyActionsLogs('ActionsLog', LOGS_COUNT, {
                action: ActionLogAction.create,
            });

            await factoryService.createManyActionsLogs('ActionsLog', 2, {
                action: ActionLogAction.update,
            });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                action: ActionLogAction.create,
                pageSize: 10,
            });

            expect(ACTUAL_LOGS.rows).toIncludeSameMembers(LOGS);
            expect(ACTUAL_LOGS.count).toBe(LOGS_COUNT);
        });

        it('Может фильтровать по сущности', async () => {
            const LOGS_COUNT = 5;
            const LOGS = await factoryService.createManyActionsLogs('ActionsLog', LOGS_COUNT, {
                entity: ActionLogEntity.post,
            });

            await factoryService.createManyActionsLogs('ActionsLog', 2, {
                entity: ActionLogEntity.tag,
            });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                entity: ActionLogEntity.post,
                pageSize: 10,
            });

            expect(ACTUAL_LOGS.rows).toIncludeSameMembers(LOGS);
            expect(ACTUAL_LOGS.count).toBe(LOGS_COUNT);
        });

        it('Может фильтровать по "urlPart"', async () => {
            await factoryService.createManyActionsLogs('ActionsLog', 5);
            const URL_PART = 'ipoteka';
            const LOG = await factoryService.createActionsLog('ActionsLog', { urlPart: URL_PART });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                urlPart: URL_PART,
                pageSize: 10,
            });

            expect(ACTUAL_LOGS).toEqual({
                rows: [LOG],
                count: 1,
            });
        });

        it('Может фильтровать по нижней дате создания', async () => {
            await factoryService.createManyActionsLogs('ActionsLog', 2, { timestamp: '2021-01-10T20:10:30.000Z' });
            const LOGS_COUNT = 3;
            const LOGS = await factoryService.createManyActionsLogs('ActionsLog', LOGS_COUNT, {
                timestamp: '2021-01-25T20:10:30.000Z',
            });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                createDateFrom: '2021-01-20T20:10:30.000Z',
                pageSize: 10,
            });

            expect(ACTUAL_LOGS.rows).toIncludeSameMembers(LOGS);
            expect(ACTUAL_LOGS.count).toBe(LOGS_COUNT);
        });

        it('Может фильтровать по верхней дате создания', async () => {
            await factoryService.createManyActionsLogs('ActionsLog', 2, { timestamp: '2021-01-25T20:10:30.000Z' });
            const LOGS_COUNT = 3;
            const LOGS = await factoryService.createManyActionsLogs('ActionsLog', LOGS_COUNT, {
                timestamp: '2021-01-10T20:10:30.000Z',
            });

            const ACTUAL_LOGS = await actionsLogService.findAndCountAll({
                createDateTo: '2021-01-20T20:10:30.000Z',
                pageSize: 10,
            });

            expect(ACTUAL_LOGS.rows).toIncludeSameMembers(LOGS);
            expect(ACTUAL_LOGS.count).toBe(LOGS_COUNT);
        });
    });

    describe('create', () => {
        it('Создает и возвращает модель лога', async () => {
            const { ACTIONS_LOG_ATTRIBUTES } = getFixtures(fixtures);

            const LOG = await actionsLogService.create(ACTIONS_LOG_ATTRIBUTES);

            expect(LOG).toBeInstanceOf(ActionsLogModel);
            expect(LOG.toJSON()).toEqual({
                id: 1,
                author: 'rbuslov',
                action: ActionLogAction.publish,
                entity: ActionLogEntity.post,
                urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
                timestamp: new Date(DATE_NOW),
            });
        });
    });

    describe('createMany', () => {
        it('Создает и возвращает массив моделей логов', async () => {
            const { ACTIONS_LOG_ATTRIBUTES_1, ACTIONS_LOG_ATTRIBUTES_2, ACTIONS_LOG_ATTRIBUTES_3 } =
                getFixtures(fixtures);

            const LOGS = await actionsLogService.createMany([
                ACTIONS_LOG_ATTRIBUTES_1,
                ACTIONS_LOG_ATTRIBUTES_2,
                ACTIONS_LOG_ATTRIBUTES_3,
            ]);

            expect(LOGS).toMatchSnapshot();
        });
    });

    describe('updatePostKey', () => {
        it('Возвращает 0, если логи поста не найдены', async () => {
            const { POST_ACTIONS_LOG_ATTRIBUTES_1, POST_ACTIONS_LOG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const OLD_URL_PART = 'old-url-part';
            const NEW_URL_PART = 'some-new-url-part';

            await ActionsLogModel.bulkCreate([POST_ACTIONS_LOG_ATTRIBUTES_1, POST_ACTIONS_LOG_ATTRIBUTES_2]);

            const result = await actionsLogService.updatePostKey({
                oldUrlPart: OLD_URL_PART,
                newUrlPart: NEW_URL_PART,
            });

            expect(result).toBe(0);
        });

        it('Обновляет urlPart и возвращает количество обновленных записей', async () => {
            const {
                POST_ACTIONS_LOG_ATTRIBUTES_1,
                POST_ACTIONS_LOG_ATTRIBUTES_2,
                POST_ACTIONS_LOG_ATTRIBUTES_3,
                TAG_ACTIONS_LOG_ATTRIBUTES_1,
            } = getFixtures(fixtures);

            const OLD_URL_PART = 'old-url-part';
            const NEW_URL_PART = 'some-new-url-part';

            await ActionsLogModel.bulkCreate([
                { ...POST_ACTIONS_LOG_ATTRIBUTES_1, urlPart: OLD_URL_PART },
                { ...POST_ACTIONS_LOG_ATTRIBUTES_2, urlPart: OLD_URL_PART },
                POST_ACTIONS_LOG_ATTRIBUTES_3,
                TAG_ACTIONS_LOG_ATTRIBUTES_1,
            ]);

            const RESULT = await actionsLogService.updatePostKey({
                oldUrlPart: OLD_URL_PART,
                newUrlPart: NEW_URL_PART,
            });
            const NEW_LOGS = await ActionsLogModel.findAll({
                where: {
                    urlPart: NEW_URL_PART,
                },
                order: ['id'],
            });

            expect(RESULT).toBe(2);
            expect(NEW_LOGS).toMatchSnapshot();
        });

        it('Обновляет urlPart только у постов', async () => {
            const { POST_ACTIONS_LOG_ATTRIBUTES_1, POST_ACTIONS_LOG_ATTRIBUTES_2, TAG_ACTIONS_LOG_ATTRIBUTES_1 } =
                getFixtures(fixtures);

            const OLD_URL_PART = 'old-url-part';
            const NEW_URL_PART = 'some-new-url-part';

            await ActionsLogModel.bulkCreate([
                { ...POST_ACTIONS_LOG_ATTRIBUTES_1, urlPart: OLD_URL_PART },
                { ...POST_ACTIONS_LOG_ATTRIBUTES_2, urlPart: OLD_URL_PART },
                { ...TAG_ACTIONS_LOG_ATTRIBUTES_1, urlPart: OLD_URL_PART },
            ]);

            const RESULT = await actionsLogService.updatePostKey({
                oldUrlPart: OLD_URL_PART,
                newUrlPart: NEW_URL_PART,
            });
            const NEW_LOGS = await ActionsLogModel.findAll({
                where: {
                    urlPart: NEW_URL_PART,
                },
                order: ['id'],
            });

            expect(RESULT).toBe(2);
            expect(NEW_LOGS).toMatchSnapshot();
        });
    });
});
