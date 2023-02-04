const config = require('../config.js');
const urljoin = require('url-join');
const {authBrowser, catchErrors} = require('../index.js');

jest.setTimeout(config.jestTimeout);

const NEIGHBOR_BANNERHASH = '00003d86442db96bce6a89a5832a5a88';


let pageSpecs = [
    {name: 'Index page', path: '/', expectedPath: '/index'},
    {name: 'Banners page', path: '/allbanners'},
    {name: 'Changes page', path: '/changes'},
    {
        name: 'Operations page',
        path: '/operations',
        ignoreMessages: [
            'Failed to load resource: the server responded with a status of 403 (Forbidden)',
            'Failed to fetch all operations JSHandle@error',
        ],
    },
];

describe('Smoke', () => {
    console.log('Running smoke tests with config', {...config, password: '******'});

    authBrowser();
    pageSpecs.forEach((pageSpec) => {
        describe(pageSpec.name, () => {
            catchErrors(pageSpec.ignoreMessages || []);

            it(pageSpec.name, async () => {
                const response = await page.goto(urljoin(config.multikHost, pageSpec.path), {
                    waitUntil: ['load', 'networkidle0'],
                    timeout: config.navigationTimeout,
                });
                expect(response.status()).toBe(200);

                const expectedPath = pageSpec.expectedPath || pageSpec.path;
                const checkUrl = await page.evaluate(() => location.pathname);
                expect(checkUrl).toBe(expectedPath);
            });
        });
    });

    describe('Neighbors', () => {
        catchErrors();
        it('Check nighbor api works correctly', async () => {
            let response = await page.goto(urljoin(config.multikHost, '/'), {
                waitUntil: ['load', 'networkidle0'],
                timeout: config.navigationTimeout,
            });
            expect(response.status()).toBe(200);
            await page.focus('.multik-filter-input[data-filter-name="bannerhash"] .Textinput input');
            await page.keyboard.type(neighborBannerhash);
            await page.keyboard.type(String.fromCharCode(13)); // hit Enter

            let row = await page.waitForSelector(`.multik-banners-table tr[data-bannerhash="${NEIGHBOR_BANNERHASH}"]`, {
                timeout: config.navigationTimeout,
            });
            const checkbox = await row.$('.Checkbox-Box');
            expect(checkbox).not.toBeNull();
            await checkbox.click();

            let button = await page.$('.IndexComponent-buttons-container #find-neighbors-button');
            expect(await button.evaluate((button) => button.disabled)).toBe(false);
            await button.click();

            await page.waitForSelector('.multik-neighbors-container .multik-banners-table', {
                timeout: config.navigationTimeout,
            });
            row = await page.$(`.multik-neighbors-table-container tr[data-bannerhash="${NEIGHBOR_BANNERHASH}"]`);
            expect(row).not.toBeNull();
            const rows = await page.$$('.multik-neighbors-table-container tr.multik-banner-main-row');
            expect(rows.length).toBeGreaterThan(1);
        });
    });
});
