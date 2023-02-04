'use strict';

jest.mock('child_process');

const childProcess = require('child_process');
const getChangedFilesFromDiff = require('../src/lib/getChangedFilesFromDiff');

it('Должен вернуть список файлов из диффа между коммитами', () => {
    childProcess.execSync.mockImplementationOnce(
        // eslint-disable-next-line max-len
        () => `www-mobile/package.json\nwww-desktop-compare/package.json\n"auto-core/some/file/path/with/quoptes/какой-то-файл"`,
    );
    expect(getChangedFilesFromDiff('010b3f273ac53bd031fdca71493d7f533cdc6809', 'ce9d0831f095145bcc189a8a31c49595d23dddec')).toEqual([
        'www-mobile/package.json',
        'www-desktop-compare/package.json',
        'auto-core/some/file/path/with/quoptes/какой-то-файл"',
    ]);
});
