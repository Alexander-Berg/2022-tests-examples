import * as net from 'net';
import * as http from 'http';
import * as express from 'express';
import {TvmDaemon} from 'tests/tvm-daemon';

let tvmDaemon: TvmDaemon;

async function startServer(app: express.Application): Promise<[http.Server, string]> {
    const server = http.createServer(app);
    await new Promise((resolve) => server.listen(resolve));
    tvmDaemon = await TvmDaemon.start();

    const port = (server.address() as net.AddressInfo).port;
    const origin = `http://localhost:${port}`;

    return [server, origin];
}

function stopServer(server: http.Server): Promise<[void, void]> {
    return Promise.all([
        tvmDaemon.stop(),
        new Promise<void>((resolve, reject) => {
            server.close((err) => {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            });
        })
    ]);
}

export {startServer, stopServer};
