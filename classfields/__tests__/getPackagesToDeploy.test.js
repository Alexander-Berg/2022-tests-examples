'use strict';

jest.mock('child_process');

const childProcess = require('child_process');
const getPackagesToDeploy = require('../src/scripts/getPackagesToDeploy');

const DEPLOYABLE_PACKAGES_MOCK = [
    {
        'package': 'www-desktop',
        service: 'af-desktop',
        deps: [ 'auto-core' ],
    },
    {
        'package': 'www-mobile',
        service: 'af-mobile',
        deps: [ 'auto-core' ],
    },
    {
        'package': 'www-desktop-compare',
        service: 'af-desktop-compare',
        deps: [ 'auto-core' ],
    },
];

afterEach(() => {
    jest.clearAllMocks();
});

it('Должен вернуть список пакетов и сервисов для принудительно установленного списка пакетов', () => {
    childProcess.execSync.mockImplementation(() => `www-mobile/package.json\nwww-desktop-compare/package.json`);
    expect(
        getPackagesToDeploy(
            DEPLOYABLE_PACKAGES_MOCK,
            [ 'www-desktop' ],
            '010b3f273ac53bd031fdca71493d7f533cdc6809',
            'ce9d0831f095145bcc189a8a31c49595d23dddec',
        ),
    ).toEqual(
        [
            {
                'package': 'www-desktop',
                service: 'af-desktop',
            },
        ],
    );
});

it('Должен вернуть список пакетов и сервисов для измененных файлов', () => {
    childProcess.execSync.mockImplementation(() => `www-mobile/package.json\nwww-desktop-compare/package.json`);
    expect(
        getPackagesToDeploy(
            DEPLOYABLE_PACKAGES_MOCK,
            [ ],
            '010b3f273ac53bd031fdca71493d7f533cdc6809',
            'ce9d0831f095145bcc189a8a31c49595d23dddec',
        ),
    ).toEqual(
        [
            {
                'package': 'www-mobile',
                service: 'af-mobile',
            },
            {
                'package': 'www-desktop-compare',
                service: 'af-desktop-compare',
            },
        ],
    );
});


it('Должен вернуть список зависимых от измененных файлов пакетов и сервисов', () => {
    childProcess.execSync.mockImplementation(() => `auto-core/package.json`);
    expect(
        getPackagesToDeploy(
            DEPLOYABLE_PACKAGES_MOCK,
            [],
            '010b3f273ac53bd031fdca71493d7f533cdc6809',
            'ce9d0831f095145bcc189a8a31c49595d23dddec',
        ),
    ).toEqual(
        [
            {
                'package': 'www-desktop',
                service: 'af-desktop',
            },
            {
                'package': 'www-mobile',
                service: 'af-mobile',
            },
            {
                'package': 'www-desktop-compare',
                service: 'af-desktop-compare',
            },
        ],
    );
});
