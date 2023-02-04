"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TvmDaemon = void 0;
const child_process_1 = require("child_process");
const util_1 = require("util");
const execFileAsync = util_1.promisify(child_process_1.execFile);
class TvmDaemon {
    constructor(subprocess) {
        this._exited = false;
        this._subprocess = subprocess;
        subprocess.on('exit', () => {
            this._exited = true;
        });
    }
    static async start() {
        return new Promise((resolve, reject) => {
            const subprocess = child_process_1.spawn('node_modules/.bin/tvmtool', [
                '--unittest',
                '--config',
                '.tvm-test.json'
            ]);
            subprocess.on('error', reject);
            subprocess.on('exit', (code) => {
                reject(new Error(`TVM daemon process exited with code ${code}`));
            });
            subprocess.stderr.on('data', (data) => {
                // tslint:disable-next-line:no-console
                console.error(data.toString());
            });
            // Give daemon some time to initialize self.
            setTimeout(() => resolve(new TvmDaemon(subprocess)), 500);
        });
    }
    async stop() {
        if (this._exited) {
            return;
        }
        // Node.js wrapper for `tvmtool-bin` doesn't forward signals to child process.
        await execFileAsync('pkill', ['-P', this._subprocess.pid.toString()]);
    }
}
exports.TvmDaemon = TvmDaemon;
