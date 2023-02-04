import * as http from 'http';

import ajaxStat from '../src/stat_helpers/ajax';
import { StatsManager } from '../src/StatsManager';
import { first } from '../src/utils';

const s = new StatsManager();

class A {
    constructor() {
        this.fetch = ajaxStat(s, this.fetch, 'test');
        this.execute = ajaxStat(s, this.execute, 'test');
        this.run = ajaxStat(s, this.run, 'test');
    }

    fetch(query) {
        const url = 'http://localhost:6666' + query;

        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('GET', url);
            xhr.onreadystatechange = () => {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status === 200) {
                        resolve(xhr);
                    } else {
                        reject(xhr);
                    }
                }
            };
            xhr.send();
        });
    }

    run(throwError = false) {
        return new Promise((resolve, reject) => {
            if (throwError) {
                return reject();
            }

            return resolve();
        });
    }

    execute() {
        /*empty*/
    }
}

beforeEach(() => {
    s.reset();
});

let server: any;

beforeAll(() => {
    const requestHandler = (request, response) => {
        response.setHeader('Access-Control-Allow-Origin', '*');

        const { url } = request;

        if (url === '/404') {
            response.statusCode = 404;
        }

        response.end();
    };

    server = http.createServer(requestHandler);
    server.listen(6666);
});

afterAll(() => {
    server.close();
});

it('should collect success request stats', done => {
    const a = new A();

    a.fetch('/').then(() => {
        const [timeFormat, timeValues] = s.getHistogram('test.time').getYasmFormat();
        expect(timeFormat).toEqual('hhhh');
        expect(timeValues.length).toEqual(1);
        expect(first(timeValues) > 0).toEqual(true);

        const [successFormat, successValues] = s.getCounter('test.200.count').getYasmFormat();
        expect(successFormat).toEqual('mmmm');
        expect(successValues).toEqual(1);

        done();
    });
});

it('should ignore incorrect behavior with simple execution', () => {
    const a = new A();
    a.execute();
    expect(s.getYasmSignals()).toEqual([]);
});

it('should ignore incorrect with resolved promises', done => {
    const a = new A();
    a.run().then(done);
});

it('should ignore incorrect with rejected promises', done => {
    const a = new A();
    a.run(true).catch(() => {
        expect(s.getYasmSignals().map(({ name }) => name)).toEqual([
            'test.failure.time_hhhh',
            'test.failure.count_mmmm'
        ]);
        done();
    });
});

it('should collect failed request stats', done => {
    const a = new A();

    a.fetch('/404').then(
        () => {
            /*empty*/
        },
        e => {
            const [timer, ...counters] = s.getYasmSignals();

            expect(counters).toEqual([
                { name: 'test.failure.count_mmmm', val: 1 },
                { name: 'test.4xx.count_mmmm', val: 1 }
            ]);

            done();
        }
    );
});
