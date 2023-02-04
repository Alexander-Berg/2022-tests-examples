module.exports = {
    browserContext: 'default',
    launch: {
        dumpio: true,
        headless: process.env.PUPPETEER_HEADLESS !== 'false',
        ignoreHTTPSErrors: process.env.PUPPETEER_IGNORE_HTTPS === 'true',
        args: ['--window-size=1920,1080'],
        defaultViewport: null,
    },
};
