const noop = () => {};

const loggerMock = {
    info: noop,
};

jest.mock('../../lib/shivaDeployAsyncRun', () => jest.fn(() => Promise.resolve()));
const shivaDeployAsyncRunMock = require('../../lib/shivaDeployAsyncRun');

jest.mock('../../lib/shivaDeployStop', () => jest.fn(() => Promise.resolve()));
const shivaDeployStopMock = require('../../lib/shivaDeployStop');

jest.mock('../../lib/doRequest', () => jest.fn());
const doRequestMock = require('../../lib/doRequest');

jest.mock('../../lib/octokit/handleCreatePrHook', () => jest.fn(() => Promise.resolve()));
const handleCreatePrHookMock = require('../../lib/octokit/handleCreatePrHook');

const { BRANCH_AUTOTEST } = require('./consts');

const handler = require('./handler');

describe('деплой в автотест', () => {
    let call;
    beforeEach(() => {
        call = {
            request: {
                deployment: {
                    service_name: 'af-desktop',
                    layer: 'TEST',
                    version: '10.10',
                    type: 'RUN',
                },
            },
        };

        shivaDeployAsyncRunMock.mockClear();
    });

    afterEach(() => {
        shivaDeployAsyncRunMock.mockClear();
    });

    it('должен вызвать деплой', () => {
        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployAsyncRunMock).toHaveBeenCalledWith({
            branch: 'autotest',
            layer: 'prod',
            name: 'af-desktop',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('не должен вызвать деплой для неизвестного сервиса', () => {
        call.request.deployment.service_name = 'af-unknown-service';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой, если указан бранч', () => {
        call.request.deployment.branch = 'AUTORUFRONT-10101';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).not.toHaveBeenCalledWith({
            branch: 'autotest',
            layer: 'prod',
            name: 'af-desktop',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('не должен вызвать деплой, если layer !== TEST', () => {
        call.request.deployment.layer = 'PROD';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой, если type не RUN или RESTART', () => {
        call.request.deployment.type = 'PROMOTE';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой в автотест, если не указана версия', () => {
        delete call.request.deployment.version;

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });
});

describe('деплой в бранч', () => {
    let call;
    beforeEach(() => {
        call = {
            request: {
                deployment: {
                    branch: 'AUTORUFRONT-10101',
                    service_name: 'af-desktop',
                    layer: 'TEST',
                    version: '10.10',
                    type: 'RUN',
                },
            },
        };

        shivaDeployAsyncRunMock.mockClear();
    });

    afterEach(() => {
        shivaDeployAsyncRunMock.mockClear();
    });

    it('должен вызвать деплой', () => {
        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployAsyncRunMock).toHaveBeenCalledWith({
            branch: 'AUTORUFRONT-10101',
            comment: undefined,
            issues: undefined,
            layer: 'prod',
            name: 'af-desktop',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('должен пробросить issues в вызов деплоя', () => {
        call.request.deployment.issues = [ 'AUTORUFRONT-10101' ];

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployAsyncRunMock).toHaveBeenCalledWith({
            branch: 'AUTORUFRONT-10101',
            comment: undefined,
            issues: [ 'AUTORUFRONT-10101' ],
            layer: 'prod',
            name: 'af-desktop',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('не должен вызвать деплой для бранча production', () => {
        call.request.deployment.branch = 'production';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой, если не указан бранч', () => {
        delete call.request.deployment.branch;

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).not.toHaveBeenCalledWith({
            comment: undefined,
            issues: undefined,
            layer: 'prod',
            name: 'af-desktop',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('должен вызвать деплой для неизвестного сервиса', () => {
        call.request.deployment.service_name = 'af-unknown-service';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployAsyncRunMock).toHaveBeenCalledWith({
            branch: 'AUTORUFRONT-10101',
            comment: undefined,
            issues: undefined,
            layer: 'prod',
            name: 'af-unknown-service',
            trafficShare: false,
            version: '10.10',
        });
    });

    it('не должен вызвать деплой, если layer !== TEST', () => {
        call.request.deployment.layer = 'PROD';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой, если type не RUN или RESTART', () => {
        call.request.deployment.type = 'PROMOTE';

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать деплой в бранч, если не указана версия', () => {
        delete call.request.deployment.version;

        handler(call, noop, loggerMock);

        expect(shivaDeployAsyncRunMock).toHaveBeenCalledTimes(0);
    });
});

describe('остановка деплоя в автотест', () => {
    let call;
    beforeEach(() => {
        call = {
            request: {
                deployment: {
                    service_name: 'af-desktop',
                    layer: 'PROD',
                    version: '10.10',
                    type: 'RUN',
                },
            },
        };

        shivaDeployStopMock.mockClear();
    });

    afterEach(() => {
        shivaDeployStopMock.mockClear();
    });

    it('должен вызвать остановку', () => {
        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployStopMock).toHaveBeenCalledWith({
            branch: 'autotest',
            comment: 'Stop branch autotest after release',
            layer: 'prod',
            name: 'af-desktop',
        });
    });

    it('должен вызвать остановку для deployment.type === PROMOTE', () => {
        call.request.deployment.type = 'PROMOTE';

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(1);
        expect(shivaDeployStopMock).toHaveBeenCalledWith({
            branch: 'autotest',
            comment: 'Stop branch autotest after release',
            layer: 'prod',
            name: 'af-desktop',
        });
    });

    it('не должен вызвать для неизвестного сервиса', () => {
        call.request.deployment.service_name = 'af-unknown-service';

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать, если указан бранч', () => {
        call.request.deployment.branch = 'AUTORUFRONT-10101';

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать, если layer !== PROD', () => {
        call.request.deployment.layer = 'TEST';

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать, если type не RUN или PROMOTE', () => {
        call.request.deployment.type = 'RESTART';

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать, если не указана версия', () => {
        delete call.request.deployment.version;

        handler(call, noop, loggerMock);

        expect(shivaDeployStopMock).toHaveBeenCalledTimes(0);
    });
});

describe('автотесты', () => {
    let call;
    beforeEach(() => {
        call = {
            request: {
                deployment: {
                    branch: BRANCH_AUTOTEST,
                    service_name: 'af-desktop',
                    layer: 'PROD',
                    version: '10.10',
                    type: 'RUN',
                },
            },
        };

        doRequestMock.mockClear();
    });

    afterEach(() => {
        doRequestMock.mockClear();
    });

    it('должен запустить сборки с автотестами', () => {
        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(1);
        expect(doRequestMock).toHaveBeenCalledWith(
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Autoru_Desktop_RunAllDesktopTests&branch=arcadia%2Ftrunk',
        );
    });

    it('должен запустить несколько сборок с автотестами', () => {
        call.request.deployment.service_name = 'af-desktop-lk';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(2);
        expect(doRequestMock).toHaveBeenCalledWith(
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Autoru_Desktop_Lk_RedirectsDesktopLkToMobile&branch=arcadia%2Ftrunk',
        );
        expect(doRequestMock).toHaveBeenCalledWith(
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Autoru_Desktop_Lk_WebTests&branch=arcadia%2Ftrunk',
        );

    });

    it('должен запустить сборки с автотестами, если deployment.type === RESTART', () => {
        call.request.deployment.type = 'RESTART';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(1);
        expect(doRequestMock).toHaveBeenCalledWith(
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Autoru_Desktop_RunAllDesktopTests&branch=arcadia%2Ftrunk',
        );
    });

    it('не должен запустить сборку, если layer !== PROD', () => {
        call.request.deployment.layer = 'TEST';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(0);
    });
    it('не должен запустить сборку, если ветка не автотест и нет метаданных run_autotests', () => {
        call.request.deployment.branch = 'AUTORUFRONT-10101';
        call.request.deployment.userMetadata = 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_desktop:master';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(0);
    });

    it('должен запустить сборку, если ветка не автотест и есть метаданные run_autotests', () => {
        call.request.deployment.branch = 'AUTORUFRONT-10101';
        call.request.deployment.userMetadata = 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_desktop:master run_autotests';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(1);
        expect(doRequestMock).toHaveBeenCalledWith(
            // eslint-disable-next-line max-len
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Autoru_DesktopWebTest&branch=arcadia%2Ftrunk&env.branch.cookie.value=AUTORUFRONT-10101',
        );
    });

    it('не должен запустить сборку, если deployment.type не RUN и не RESTART', () => {
        call.request.deployment.type = 'PROMOTE';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(0);
    });

    it('не должен запустить сборку для неизвестного сервиса', () => {
        call.request.deployment.service_name = 'af-unknown-service';

        handler(call, noop, loggerMock);

        expect(doRequestMock).toHaveBeenCalledTimes(0);
    });
});

describe('PR-хук', () => {
    let call;
    beforeEach(() => {
        call = {
            request: {
                deployment: {
                    branch: 'release_12.09.2018',
                    userMetadata: 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master',
                    service_name: 'af-forms',
                    layer: 'TEST',
                    version: '10.10',
                    type: 'RUN',
                },
            },
        };

        handleCreatePrHookMock.mockClear();
    });

    afterEach(() => {
        handleCreatePrHookMock.mockClear();
    });

    it('не должен вызывать хук, если нет опций', () => {
        delete call.request.deployment.userMetadata;

        handler(call, noop, loggerMock);

        expect(handleCreatePrHookMock).toHaveBeenCalledTimes(0);
    });

    it('должен вызывать хук с параметрами из userMetadata', () => {
        handler(call, noop, loggerMock);

        expect(handleCreatePrHookMock).toHaveBeenCalledTimes(1);
        expect(handleCreatePrHookMock).toHaveBeenCalledWith({
            addBranchLabel: false,
            base: 'master',
            head: 'release_12.09.2018_forms',
            labels: [],
            repo: 'autoru-frontend',
            service_name: 'af-forms',
        });
    });

    it('должен вызывать хук с лейблом branch:testing, если есть add_branch_label в метаданных и layer === TEST', () => {
        call.request.deployment.userMetadata = 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_branch_label';

        handler(call, noop, loggerMock);

        expect(handleCreatePrHookMock).toHaveBeenCalledTimes(1);
        expect(handleCreatePrHookMock).toHaveBeenCalledWith({
            addBranchLabel: true,
            base: 'master',
            head: 'release_12.09.2018_forms',
            labels: [ 'branch:testing' ],
            repo: 'autoru-frontend',
            service_name: 'af-forms',
        });
    });

    it('должен вызывать хук с лейблом branch:stable, если есть add_branch_label в метаданных и layer === PROD', () => {
        call.request.deployment.userMetadata = 'DEPLOY_HOOK create_pr=autoru-frontend:release_12.09.2018_forms:master add_branch_label';
        call.request.deployment.layer = 'PROD';

        handler(call, noop, loggerMock);

        expect(handleCreatePrHookMock).toHaveBeenCalledTimes(1);
        expect(handleCreatePrHookMock).toHaveBeenCalledWith({
            addBranchLabel: true,
            base: 'master',
            head: 'release_12.09.2018_forms',
            labels: [ 'branch:stable' ],
            repo: 'autoru-frontend',
            service_name: 'af-forms',
        });
    });
});
