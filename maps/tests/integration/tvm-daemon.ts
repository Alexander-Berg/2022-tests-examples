import {spawn, ChildProcess, execFile} from 'child_process';
import {promisify} from 'util';

const execFileAsync = promisify(execFile);

export class TvmDaemon {
    private _subprocess: ChildProcess;
    private _exited: boolean = false;

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
                '--config', '.tvm.unittest.json',
                '--port', process.env.MAPS_DEPLOY_TVM_TOOL_PORT!
            ]);

            subprocess.on('error', reject);
            // Give daemon some time to initialize self.
            setTimeout(() => resolve(new TvmDaemon(subprocess)), 500);
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
