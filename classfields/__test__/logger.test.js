const { execSync } = require('child_process');
const path = require('path');

it('должен пивать логи в формате vertis-logs', () => {
    const file = path.resolve(__dirname, 'logger_child.js');
    const result = execSync('node ' + file, { cwd: __dirname, encoding: 'utf8' });

    expect(result).toEqual([
        '{"_level":"INFO","_time":"2020-10-18T14:00:50.000Z","_message":"info"}',
        '{"_level":"WARN","_time":"2020-10-18T14:00:50.000Z","_message":"warn"}',
        '{"_level":"ERROR","_time":"2020-10-18T14:00:50.000Z","_message":"error"}',
        '{"_level":"INFO","_time":"2020-10-18T14:00:50.000Z","foo":"bar","_message":"info with obj"}',
        '{"_level":"INFO","_time":"2020-10-18T14:00:50.000Z","_context":"context","_message":"child info"}',
        '',
    ].join('\n'));
});
