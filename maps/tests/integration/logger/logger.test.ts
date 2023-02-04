import {spawnSync} from 'child_process';
import {expect} from 'chai';

const TIMESTAMP_RE_STR: string = '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z';

describe('Logger', () => {
    it('should log messages by levels', () => {
        const result = spawnSync(process.execPath, [`${__dirname}/logger.js`]);
        expect(result.status).to.equal(0);

        const stdout = result.stdout.toString().split('\n');
        const stderr = result.stderr.toString().split('\n');

        expect(stdout).to.have.length(5, 'Expect 5 lines in stdout');
        expect(stdout[0]).to.match(createLogMessageRe('silly: some silly message'));
        expect(stdout[1]).to.match(createLogMessageRe('debug: some debug message'));
        expect(stdout[2]).to.match(createLogMessageRe('verbose: some verbose message'));
        expect(stdout[3]).to.match(createLogMessageRe('info: some info message'));
        expect(stdout[4]).to.equal('');

        expect(stderr).to.have.length(3, 'Expect 3 lines in stderr');
        expect(stderr[0]).to.match(createLogMessageRe('warn: some warn message'));
        expect(stderr[1]).to.match(createLogMessageRe('error: some error message'));
        expect(stderr[2]).to.equal('');
    });
});

function createLogMessageRe(message: string): RegExp {
    return new RegExp(`^${TIMESTAMP_RE_STR} ${message}$`);
}
