'use strict';

jest.mock('../src/lib/shivaRequest');
jest.mock('child_process');
jest.mock('fs', () => {
    return {
        readFileSync: jest.fn(),
    };
});

const shivaRequest = require('../src/lib/shivaRequest');
const childProcess = require('child_process');
const { getTasksInMaster } = require('../src');
const fs = require('fs');

afterEach(() => {
    jest.clearAllMocks();
});

it('должен забирать прод-тег из shiva, передавать в его git log и форматировать ответ в пригодный для docker-autorelease', async() => {
    shivaRequest.mockImplementation(() => Promise.resolve('67d9a80dbcd'));
    childProcess.execSync.mockImplementation(() => `CLASSFRONT-1569\nCLASSFRONT-1637\nCLASSFRONT-1646`);

    const result = await getTasksInMaster({ serviceName: 'gf-desktop', certPath: '/' });

    expect(childProcess.execSync).toHaveBeenCalledWith(
        'git log --pretty=%s gf-desktop_67d9a80dbcd..origin/master | grep -oE \'[A-Z]+-[0-9]+\' | sort | uniq',
        { encoding: 'utf-8' },
    );

    expect(result).toEqual({ tasks: 'CLASSFRONT-1569,CLASSFRONT-1637,CLASSFRONT-1646' });
});

it('должен выбрасывать ошибку, если не передано имя сервиса', async() => {
    expect.assertions(1);

    await expect(() => getTasksInMaster({ certPath: '/' })).toThrow(/serviceName is undefined/);
});

it('должен отдавать правильный message и не падать, если не найден продовый тег', async() => {
    expect.assertions(1);

    shivaRequest.mockImplementation(() => Promise.resolve(undefined));

    const result = await getTasksInMaster({ serviceName: 'gf-desktop', certPath: '/' });

    expect(result).toEqual({ message: `Production version not found for service gf-desktop` });
});

it('должен передавать серт в shivaRequest', async() => {
    fs.readFileSync.mockImplementation((path) => path === '/' ? 'cert' : undefined);

    await getTasksInMaster({ serviceName: 'gf-desktop', certPath: '/' });

    expect(shivaRequest).toHaveBeenCalledWith({
        ca: 'cert',
        body: '{"service_name":"gf-desktop","layer":"Prod"}',
        path: '/version',
    });
});
