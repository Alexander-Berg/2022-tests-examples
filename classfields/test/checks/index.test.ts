import { Logger } from '../mocks/Logger';
import { stClient } from '../mocks/StClient';
import { tgBotClient } from '../mocks/TgBotClient';

jest.mock('../../modules/StClient', () => ({ stClient }));
jest.mock('../../modules/TgBotClient', () => ({ tgBotClient }));
jest.mock('../../modules/Logger', () => ({ Logger }));

import nock from 'nock';
import { app } from '../../app';
import { Probot, ProbotOctokit } from 'probot';

import payload from './fixtures/checks.json';
import payloadRelease from './fixtures/checks_release.json';

import fs from 'fs';
import path from 'path';

const privateKey = fs.readFileSync(
    path.join(__dirname, '../mock-cert.pem'),
    'utf-8',
);

describe('Получение эвента check_suite.completed', () => {
    let probot: Probot;

    async function processResponse(checkSuites: Array<unknown>, payload: unknown, statusesResponse: string) {
        nock('https://api.github.com')
            .post('/app/installations/2/access_tokens')
            .times(2)
            .reply(200, {
                token: 'test',
                permissions: {
                    check_suite: 'read',
                    status: 'read',
                },
            })
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/commits/e9c82d10e14cb23d73e800bb6e975622eacdedbb/check-suites')
            .reply(200, {
                check_suites: checkSuites,
            })
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/check-suites/456/check-runs')
            .reply(200, {
                check_runs: [ { name: 'required_check' } ],
            })
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/branches/master/protection/required_status_checks')
            .reply(200, {
                contexts: [
                    'required_check',
                ],
            })
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/commits/e9c82d10e14cb23d73e800bb6e975622eacdedbb/status')
            .reply(200, {
                state: statusesResponse,
                statuses: [ { state: statusesResponse, context: 'required_check' } ],
            })
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/pulls')
            .query(true)
            .reply(200, [
                { state: 'open', html_url: 'https://github.com/YandexClassifieds/test-vertis-frontend-gh-app/pull/2',
                    head: { sha: 'e9c82d10e14cb23d73e800bb6e975622eacdedbb' } },
                { state: 'closed', html_url: 'https://github.com/YandexClassifieds/test-vertis-frontend-gh-app/pull/3',
                    head: { sha: 'e9c82d10e14cb23d73e800bb6e975622eacdedbb' } } ])
            .get('/repos/YandexClassifieds/test-vertis-frontend-gh-app/check-suites/123/check-runs')
            .reply(200, { check_runs: [
                { name: 'Check', conclusion: 'failure' },
                { name: 'Check 2', conclusion: 'failure' },
                { name: 'Check 3', conclusion: 'success' },
            ] });

        await probot.receive({ id: '123', name: 'check_suite', payload });
    }


    beforeEach(() => {
        nock.disableNetConnect();

        probot = new Probot({
            appId: 123,
            privateKey,
            // disable request throttling and retries for testing
            Octokit: ProbotOctokit.defaults({
                retry: { enabled: false },
                throttle: { enabled: false },
            }),
        });
        // Load our app into probot
        probot.load(app);
    });

    it('должен проставляться тег checks_ok, если все сьюты завершились успешно', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '456',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).toHaveBeenCalledWith('AUTORUFRONT-16', { tags: [ 'checks_ok' ] });
    });

    it('должен проставляться тег checks_not_ok, если все сьюты хотя бы один из сьютов завершился неуспешно', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'failure',
                id: '456',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).toHaveBeenCalledWith('AUTORUFRONT-16', { tags: [ 'checks_not_ok' ] });
    });

    it('должен сбрасываться тег checks_ok (с сохранением остальных тегов), если хотя бы один из сьютов завершился неуспешно', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'foo', 'checks_ok' ] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'failure',
                id: '456',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).toHaveBeenCalledWith('AUTORUFRONT-16', { tags: [ 'foo', 'checks_not_ok' ] });
    });

    it('должен сбрасываться тег checks_not_ok (с сохранением остальных тегов), если все сьюты завершились успешно', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'foo', 'checks_not_ok' ] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '456',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).toHaveBeenCalledWith('AUTORUFRONT-16', { tags: [ 'foo', 'checks_ok' ] });
    });

    it('не должны обновляться задачи, если статусы не готовы', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'foo', 'checks_not_ok' ] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '456',
            },
        ], payload, 'pending');

        expect(stClient.updateIssue).not.toHaveBeenCalled();
    });

    it('должен проставляться тег checks_not_ok, если статусы сфейлились', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'foo', 'checks_not_ok' ] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '456',
            },
        ], payload, 'failure');

        expect(stClient.updateIssue).not.toHaveBeenCalled();
    });

    it('не должны обновляться задачи, если не все сьюты завершены', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'foo', 'checks_not_ok' ] }));

        await processResponse([
            {
                status: 'in_progress',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '456',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).not.toHaveBeenCalled();
    });

    it('не должны обновляться задачи, если теги совпадают', async() => {
        stClient.getIssue.mockImplementation(() => ({ tags: [ 'checks_not_ok', 'foo' ] }));

        await processResponse([
            {
                status: 'completed',
                conclusion: 'failure',
                id: '456',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '123',
            },
        ], payload, 'success');

        expect(stClient.updateIssue).not.toHaveBeenCalled();
    });

    it('в релизной ветке должен отправлять уведомления о непрошедших чеках в телеграм', async() => {
        await processResponse([
            {
                status: 'completed',
                conclusion: 'failure',
                id: '123',
            },
            {
                status: 'completed',
                conclusion: 'success',
                id: '123456',
            },
        ], payloadRelease, 'success');

        expect(tgBotClient.autoRuReleaseChecks).toHaveBeenCalledWith({
            headBranch: '08.04.2020-desktop',
            pullRequestLink: 'https://github.com/YandexClassifieds/test-vertis-frontend-gh-app/pull/2',
            result: 'failure',
            headSha: 'e9c82d10e14cb23d73e800bb6e975622eacdedbb',
            messages: 'Check: failure\nCheck 2: failure',
        });
    });

    afterEach(() => {
        nock.cleanAll();
        nock.enableNetConnect();
        jest.resetAllMocks();
    });
});
