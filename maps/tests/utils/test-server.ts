import * as assert from 'assert';
import * as http from 'http';
import * as express from 'express';
import {AddressInfo} from 'net';

export class TestServer {
    private readonly _server: http.Server;
    private readonly _baseUrl: string;
    private _isClosed: boolean = false;

    get url(): string {
        assert(!this._isClosed, 'Server is closed');
        return this._baseUrl;
    }

    private constructor(server: http.Server) {
        this._server = server;
        const port = (server.address() as AddressInfo).port;
        this._baseUrl = `http://127.0.0.1:${port}`;
    }

    static async start(app: express.Application): Promise<TestServer> {
        const server = http.createServer(app);
        await new Promise((resolve) => {
            server.listen(resolve);
        });
        return new this(server);
    }

    stop(): Promise<void> {
        this._isClosed = true;
        return new Promise((resolve, reject) => {
            this._server.close((err: any) => {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }
}
