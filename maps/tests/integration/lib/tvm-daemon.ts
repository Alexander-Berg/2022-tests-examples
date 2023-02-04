import {ChildProcessWithoutNullStreams, execFile, spawn} from 'child_process';
import {promisify} from 'util';

const execFileAsync = promisify(execFile);

export class TvmDaemon {
    private _subprocess: ChildProcessWithoutNullStreams;
    private _exited: boolean;

    constructor(subprocess: ChildProcessWithoutNullStreams) {
        this._subprocess = subprocess;
        this._exited = false;
        subprocess.on('exit', () => {
            this._exited = true;
        });
    }

    static async start(): Promise<TvmDaemon> {
        return new Promise((resolve, reject) => {
            const subprocess = spawn('node_modules/.bin/tvmtool', [
                '--unittest',
                '--config',
                'src/tests/integration/.tvm.json'
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

    async stop() {
        if (this._exited) {
            return;
        }
        if (this._subprocess.pid) {
            // Node.js wrapper for `tvmtool-bin` doesn't forward signals to child process.
            await execFileAsync('pkill', ['-P', this._subprocess.pid.toString()]);
        }
    }
}
