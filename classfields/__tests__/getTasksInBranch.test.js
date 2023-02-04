'use strict';

jest.mock('../src/lib/shivaRequest');
jest.mock('child_process');
jest.mock('fs', () => {
    return {
        readFileSync: jest.fn(),
    };
});

const shivaRequest = require('../src/lib/shivaRequest');
const fs = require('fs');
const childProcess = require('child_process');
const { getTasksInBranch } = require('../src');

afterEach(() => {
    jest.clearAllMocks();
});

it('должен забирать прод-тег из системы деплоя и форматировать ответ в пригодный для docker-autorelease', async() => {
    shivaRequest.mockImplementation(() => Promise.resolve('67d9a80dbcd'));
    childProcess.execSync.mockImplementationOnce(() => `CLASSFRONT-1569\nCLASSFRONT-1637\nCLASSFRONT-1646`); // имитируем мастер..ветка
    childProcess.execSync.mockImplementationOnce(() => `CLASSFRONT-1569`); // имитируем мастер..прод

    const result = await getTasksInBranch({ serviceName: 'gf-desktop', certPath: '/' });

    expect(result).toEqual({ tasks: 'CLASSFRONT-1637,CLASSFRONT-1646' });
    expect(shivaRequest).toHaveBeenCalled();
});

it('должен передавать имя сервиса в git log', async() => {
    childProcess.execSync.mockImplementation(() => '');
    await getTasksInBranch({ serviceName: 'gf-desktop', certPath: '/' });

    expect(childProcess.execSync.mock.calls).toEqual([
        [ 'git log --pretty=%s origin/master..HEAD | grep -oE \'[A-Z]+-[0-9]+\' | sort | uniq',
            { encoding: 'utf-8' } ],
        [ 'git log --pretty=%s origin/master..gf-desktop_67d9a80dbcd | grep -oE \'[A-Z]+-[0-9]+\' | sort | uniq',
            { encoding: 'utf-8' } ],
    ]);
});

it('должен выбрасывать ошибку, если не передано имя сервиса', async() => {
    expect.assertions(1);

    await expect(() => getTasksInBranch({ certPath: '/' })).toThrow(/serviceName is undefined/);
});

it('должен отдавать правильный message и не падать, если не найден продовый тег', async() => {
    expect.assertions(1);

    shivaRequest.mockImplementation(() => Promise.resolve(undefined));

    const result = await getTasksInBranch({ serviceName: 'gf-desktop', certPath: '/' });

    expect(result).toEqual({ message: `Production version not found for service gf-desktop` });
});

it('должен передавать серт в shivaRequest', async() => {
    fs.readFileSync.mockImplementation((path) => path === '/' ? 'cert' : undefined);

    await getTasksInBranch({ serviceName: 'gf-desktop', certPath: '/' });

    expect(shivaRequest).toHaveBeenCalledWith({
        ca: 'cert',
        body: '{"service_name":"gf-desktop","layer":"Prod"}',
        path: '/version',
    });
});
