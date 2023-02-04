import * as path from 'path';
import {spawn, ChildProcess, execFile} from 'child_process';
import {promisify} from 'util';

const execFileAsync = promisify(execFile);

class TvmDaemon {
    private _subprocess: ChildProcess;
    private _exited = false;

    private constructor(subprocess: ChildProcess) {
        this._subprocess = subprocess;
        subprocess.on('exit', () => {
            this._exited = true;
        });
    }

    static async start(): Promise<TvmDaemon> {
        return new Promise((resolve, reject) => {
            const subprocess = spawn('node_modules/.bin/tvmtool', [
                '--unittest',
                '--config',
                path.resolve('.tvm-test.json')
            ]);
            subprocess.on('error', reject);
            subprocess.on('exit', (code) => {
                reject(new Error(`TVM daemon process exited with code ${code}`));
            });
            subprocess.stderr.on('data', (data) => {
                // eslint-disable-next-line no-console
                console.error(data.toString());
            });
            // Give daemon some time to initialize self.
            setTimeout(() => resolve(new TvmDaemon(subprocess)), 1000);
        });
    }

    async stop(): Promise<void> {
        if (this._exited) {
            return;
        }
        // Node.js wrapper for `tvmtool-bin` doesn't forward signals to child process.
        await execFileAsync('pkill', ['-P', this._subprocess.pid.toString()]);
    }
}

export {TvmDaemon};
