import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';

import { getFixtures } from '../../../tests/get-fixtures';
import { Author as AuthorModel } from '../author.model';
import { AuthorSocialNetwork as AuthorSocialNetworkModel } from '../../author-social-network/author-social-network.model';
import { AuthorModule } from '../author.module';
import { AuthorService } from '../author.service';
import { ActionsLogService } from '../../actions-log/actions-log.service';
import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';

import { fixtures } from './author.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Author service', () => {
    let testingModule: TestingModule;
    let authorService: AuthorService;
    let actionsLogService: ActionsLogService;
    let spyLogCreate: jest.SpyInstance;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [AuthorModule],
        }).compile();

        authorService = await testingModule.resolve(AuthorService);
        actionsLogService = await testingModule.resolve(ActionsLogService);
        spyLogCreate = jest.spyOn(actionsLogService, 'create');

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('findByPk', () => {
        it('Возвращает null, если автор не найден', async () => {
            const result = await authorService.findByPk(1);

            expect(result).toEqual(null);
        });

        it('Возвращает существующего автора с его соц.сетями', async () => {
            const { AUTHOR_DATA, SOCIAL_NETWORK_VK_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(
                { ...AUTHOR_DATA, socialNetworks: [SOCIAL_NETWORK_VK_DATA] },
                { include: [AuthorSocialNetworkModel] }
            );

            const result = await authorService.findByPk(author.id);

            expect(result).toMatchSnapshot();
        });
    });

    describe('findByUrlPart', () => {
        it('Возвращает null, если автор не найден', async () => {
            const result = await authorService.findByUrlPart('redakciya');

            expect(result).toEqual(null);
        });

        it('Возвращает существующего автора с его соц.сетями', async () => {
            const { AUTHOR_DATA } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: [AuthorSocialNetworkModel] });

            const result = await authorService.findByUrlPart(author.urlPart);

            expect(result).toMatchSnapshot();
        });
    });

    describe('create', () => {
        it('Создает и возвращает автора вместе с его соц.сетями', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await authorService.create({ ...AUTHOR_DATA, userLogin: USER_LOGIN });

            expect(author).toMatchSnapshot();
            expect(spyLogCreate).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.author,
                action: ActionLogAction.create,
                urlPart: author.id,
            });
        });
    });

    describe('update', () => {
        it('Обновляет и возвращает автора вместе с его соц.сетями', async () => {
            const { AUTHOR_DATA, AUTHOR_UPDATE_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const updatedAuthor = await authorService.update(author.id, {
                ...AUTHOR_UPDATE_DATA,
                userLogin: USER_LOGIN,
            });

            expect(updatedAuthor).toMatchSnapshot();
            expect(spyLogCreate).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.author,
                action: ActionLogAction.update,
                urlPart: author.id,
            });
        });
    });

    describe('delete', () => {
        it('Возвращает false, если автор не найден', async () => {
            const result = await authorService.delete(1, { userLogin: 'editor-1' });

            expect(result).toEqual(false);
        });

        it('Удаляет автора и его соц.сети', async () => {
            const { AUTHOR_DATA, USER_LOGIN } = getFixtures(fixtures);

            const author = await AuthorModel.create(AUTHOR_DATA, { include: AuthorSocialNetworkModel });

            const result = await authorService.delete(author.id, { userLogin: USER_LOGIN });

            await expect(AuthorModel.findByPk(author.id)).resolves.toBe(null);
            await expect(AuthorSocialNetworkModel.count({ where: { authorId: author.id } })).resolves.toBe(0);
            expect(result).toBe(true);
            expect(spyLogCreate).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.author,
                action: ActionLogAction.delete,
                urlPart: author.id,
            });
        });
    });
});
