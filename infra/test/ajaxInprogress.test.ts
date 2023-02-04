import { createReadStream, readFileSync, statSync } from 'fs';
import { createServer } from 'http';
import { join as joinPath } from 'path';

import puppeteer from 'puppeteer';

let server: any;
let browser: any;

beforeAll(async () => {
    const requestHandler = (request, response) => {
        response.setHeader('Access-Control-Allow-Origin', '*');

        const { url } = request;

        if (url === '/404') {
            response.statusCode = 404;
        }

        response.end();
    };

    server = createServer(requestHandler);
    server.listen(3001);
    browser = await puppeteer.launch();
});

afterAll(async () => {
    server.close();
    // await browser.close();
});

test('should collect success request stats', async () => {
    const page = await browser.newPage();
    await page.addScriptTag({ path: joinPath(__dirname, '../node_modules/rxjs/bundles/rxjs.umd.min.js') });
    await page.addScriptTag({ path: joinPath(__dirname, '../dist/index.js') });

    const result = await page.evaluate(() => {
        const Stats = yasmStats.default;
        Stats.config({ debug: true, ajax: true }).install();
        return fetch('http://localhost:3001/404').then(() => Stats.getYasmSignals());
    });

    expect(result).toEqual([{ name: 'requests.inprogress.count_hhhh', val: [1, 0] }]);
}, 60000);
