import {TvmDaemon} from './tvm-daemon';

let tvmDaemon: TvmDaemon;

async function setup() {
    tvmDaemon = await TvmDaemon.start();
}

async function teardown() {
    await tvmDaemon.stop();
}

export {setup, teardown};
