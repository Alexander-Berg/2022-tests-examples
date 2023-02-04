const querystring = require('querystring');
const vow = require('vow');
const Queue = require('vow-queue');
const spawn = require('child_process').spawn;
const Progress = require('progress');
const http = require('http');

const fs = require('fs');
const clc = require('cli-color');
const { Transform } = require('stream');

const args = require('minimist')(process.argv.slice(2));

const SERVER = `http://localhost:${process.env.MAPS_NODEJS_PORT}`;
const PAGE = `${SERVER}/tests/index.html`;
const TESTFILE_TIMEOUT = 30 * 1000;
const CRASH_ATTEMPTS = 3;

(async () => {
    const files = require('glob').sync('src/**/*.test.js').filter(x => x.startsWith(args.filter || ''));
    await waitServer();
    const info = await run(files);
    if (info.failed !== 0) {
        process.exitCode = 1;
    }
})();

async function waitServer() {
    const startTs = Date.now();
    while (true) {
        if (Date.now() - startTs > 2000) {
            throw new Error(`${SERVER} is not responding.`);
        }

        const ok = await new Promise((resolve) => {
            const req = http.request(`${SERVER}/ping`);
            req.on('response', (res) => {
                if (res.statusCode === 200) {
                    resolve(true);
                }
            });
            req.on('error', () => resolve(false));
            req.end();
        });

        if (ok) {
            return;
        }

        await new Promise((r) => setTimeout(r, 50));
    }
}

async function run(testfiles) {
    const information = [];
    const progress = new Progress('[:bar] :percent  :etas, total: :total, current: :current', {
        complete: '=',
        incomplete: ' ',
        width: 40,
        total: testfiles.length
    });

    if (args.log) {
        console.log(clc.white(clc.bold('ATTENTION: ') + 'phantomjs does not separate console.log/warn/error'));
    }

    if (!args.log) progress.render();

    const queue = new Queue({weightLimit: 5});
    testfiles.forEach((testfile) => queue.enqueue(async function () {
        try {
            const data = await runTestfile(testfile);
            information.push({
                module: testfile,
                stats: data.stats,
                data: data
            });
        } catch (err) {
            information.push({
                module: testfile,
                stats: { tests: 1, failures: 1, passes: 0 },
                data: { failures: [{ title: Array.isArray(err) ? err.join('\n') : err, err: { message: '' } }] }
            });
        } finally {
            if (!args.log) progress.tick();
        }
    }));

    queue.start();

    await new Promise(resolve => {
        const interval = setInterval(() => {
            if (queue.getStats().processedTasksCount === testfiles.length) {
                clearInterval(interval);
                resolve();
            }
        }, 10);
    });

    information.sort(function (a, b) { return a.module.localeCompare(b.module); });

    console.log(information.map((info) => {
        if (!info.stats.failures) {
            return clc.green('✔ ') + info.module + '\n';
        }
        return clc.red('✗ ') + info.module + '\n' +
            info.data.failures.map((x) => `  — ${clc.red(x.title)}\n    ${x.err.message.replace(/\n/g, '\n    ')}\n`).join('');
    }).join(''));

    const failed = information.filter(x => x.stats.failures).length;
    const passed = information.length - failed;
    console.log(`SUITES: ${failed} FAILED, ${passed} PASSED`);

    const failedCases = information.reduce((s, x) => s + x.stats.failures, 0);
    const passedCases = information.reduce((s, x) => s + x.stats.passes, 0);
    console.log(`CASES: ${failedCases} FAILED, ${passedCases} PASSED`);

    information.failed = failed;
    information.passed = passed;

    return information;
}

async function runTestfile(testfile, attemptsLeft = CRASH_ATTEMPTS - 1) {
    const reportJsonFile = `/tmp/jsapi-v2-1-testrun-${process.pid}-${Date.now()}-${testfile.replace(/\W/g, '_')}.json`;
    try { fs.unlinkSync(reportJsonFile); } catch (e) {}
    const mochaPhantomJsConfig = JSON.stringify({
        timeout: '10000',
        file: reportJsonFile
    });

    // Run mocha-phantomjs process.
    const phantomProcess = spawn(require('phantomjs').path, [
        // PhantomJS settings.
        '--ignore-ssl-errors=true',
        '--ssl-protocol=any',
        '--web-security=false', // TODO: this is not ok
        // Mocha-phantomJS runner.
        `${__dirname}/mocha-runner/mocha-runner.coffee`,
        // Mocha-phantomJS options
        PAGE + '?' + querystring.stringify({filter: testfile, debug: args.debug, grep: args.grep}),
        // Reporter.
        'json',
        mochaPhantomJsConfig
    ], {cwd: '.'});

    if (args.log) {
        // Possibly phantomjs errors, but mostly useless warnings about resource loading.
        phantomProcess.stderr
            .pipe(new SplitIntoLines())
            .pipe(new FilterLines(line => !/^Error loading resource .* server replied: Not Found$/i.test(line)))
            .pipe(new FilterLines(line => !/^Error loading resource .* Details: Operation canceled$/i.test(line)))
            .pipe(new MapLines(line => clc.yellow('[warning] ') + clc.cyan(testfile + ' | ') + line))
            .pipe(new JoinLines())
            .pipe(process.stdout);

        // console.log and company.
        phantomProcess.stdout
            .pipe(new SplitIntoLines())
            .pipe(new FilterLines(line => !/^Error: Warning: Can not convert style to image\(SECURITY_ERR: DOM Exception 18\)$/.test(line)))
            .pipe(new MapLines(line => clc.white('[console] ') + clc.cyan(testfile + ' | ') + line))
            .pipe(new JoinLines())
            .pipe(process.stdout);
    }

    let crashed = false;
    phantomProcess.stderr
        .pipe(new SplitIntoLines())
        .pipe(new class extends Transform {
            _transform(line, _encoding, callback) {
                if (line.includes('PhantomJS has crashed. Please read the crash reporting guide at')) {
                    crashed = true;
                }
                callback();
            }
        });

    const errorsFromPhantom = [];
    phantomProcess.stdout.on('data', data => {
        data = data.toString();
        const errorText = checkOnBrowserError(data.toString());
        if (errorText) {
            errorsFromPhantom.push(errorText);
        }
    });

    try {
        await new vow.Promise(resolve => phantomProcess.stdout.once('close', resolve))
            .timeout(TESTFILE_TIMEOUT);
    } catch (e) {
        if (e instanceof vow.TimedOutError) {
            phantomProcess.kill('SIGHUP');
            return vow.Promise.reject(errorsFromPhantom);
        }
    }

    try {
        const report = JSON.parse(fs.readFileSync(reportJsonFile, 'utf8'));
        fs.unlinkSync(reportJsonFile);
        return report;
    } catch (e) {
        if (crashed) {
            if (attemptsLeft > 0) {
                return runTestfile(testfile, attemptsLeft - 1);
            }
            throw new Error(`${testfile}: phantom.js crashed ${CRASH_ATTEMPTS} times in a row`);
        }

        console.error(e);
        throw new Error(`${testfile}: failed to read reported output from ${reportJsonFile}`);
    }
}


/**
 * Checks whether the text from the browser is an error
 * @param {String} text
 * @returns {String|null}
 */
function checkOnBrowserError(data) {
    const start = data.search(/(Eval|Internal|Range|Reference|Syntax|Type|URI)Error/);
    if (start === -1) {
        return null;
    }

    const end = data.indexOf('\n');
    return data.slice(start, (end === -1) ? data.length : end);
}

class SplitIntoLines extends Transform {
    constructor() {
        super();
        this._leftover = '';
    }
    _transform(data, _encoding, callback) {
        const lines = data.toString().split('\n')
        lines[0] = this._leftover + lines[0];
        this._leftover = lines.pop();
        for (const line of lines) {
            this.push(line, 'utf8');
        }
        callback();
    }
}

class MapLines extends Transform {
    constructor(mapper) {
        super();
        this._mapper = mapper;
    }
    _transform(line, _encoding, callback) {
        callback(null, this._mapper(line));
    }
}

class FilterLines extends Transform {
    constructor(predicate) {
        super();
        this._predicate = predicate;
    }
    _transform(line, _encoding, callback) {
        if (this._predicate(line)) {
            this.push(line);
        }
        callback();
    }
}

class JoinLines extends Transform {
    _transform(line, _encoding, callback) {
        callback(null, line + '\n');
    }
}
