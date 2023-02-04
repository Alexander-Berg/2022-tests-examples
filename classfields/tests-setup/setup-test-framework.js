require('expect-puppeteer');

const { toMatchImageSnapshotFn } = require('./utils');

const isDebug = Boolean(process.env.IS_DEBUG) || Boolean(process.env.VNC_DEBUG);
jest.setTimeout(isDebug ? 10005000 : 180000);

if (process.env.CI) {
    // на CI добавляем ретраи для флапающих тестов
    jest.retryTimes(3);
}

expect.extend({
    toMatchImageSnapshot: toMatchImageSnapshotFn,
});
