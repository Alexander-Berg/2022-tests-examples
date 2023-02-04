import {Application} from 'express';
import {createServer, Server} from 'http';
import {AddressInfo} from 'net';

export class TestServer {
    private readonly _server: Server;
    private readonly _baseUrl: string;

    get url(): string {
        return this._baseUrl;
    }

    private constructor(server: Server) {
        this._server = server;
        const port = (server.address() as AddressInfo).port;
        this._baseUrl = `http://127.0.0.1:${port}`;
    }

    static async start(app: Application): Promise<TestServer> {
        const server = createServer(app);
        await new Promise((resolve) => server.listen(resolve));
        return new this(server);
    }

    stop(): Promise<void> {
        return new Promise((resolve, reject) => {
            this._server.close((err?: Error) => err ? reject(err) : resolve());
        });
    }
}
