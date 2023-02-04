const urlUtils = require('url');
const config = require('./config.js');

const isAuthenticated = async (page, baseUrl, passportUrl) => {
    const updateAuthUrl = `${passportUrl}/auth/update?retpath=${baseUrl}`;
    const response = await page.goto(updateAuthUrl, {waitUntil: ['load', 'networkidle0']});
    const {hostname: targetHostname} = urlUtils.parse(updateAuthUrl);
    const {hostname: actualHostname} = urlUtils.parse(response.url());
    // if we have been redirected away from passport, most likely we're already authenticated
    return targetHostname !== actualHostname;
};

export const authenticate = async (page, baseUrl, passportUrl, login, password) => {
    try {
        const authenticated = await isAuthenticated(page, baseUrl, passportUrl);
        if (!authenticated) {
            const authUrl = `${passportUrl}/auth?mode=password&retpath=${encodeURIComponent(baseUrl)}`;

            const timestamp = Math.round(new Date().getTime() / 1000);
            const html = `
                <html>
                    <form method="POST" id="authForm" action="${authUrl}">
                        <input name="login" value="${login}">
                        <input type="password" name="passwd" value="${password}">
                        <input type="checkbox" name="twoweeks" value="no">
                        <input type="hidden" name="timestamp" value="${timestamp}">
                        <button type="submit">Login</button>
                    </form>
                <html>
            `;
            await page.setContent(html);
            await page.waitForSelector('#authForm');
            await page.click('button');
            await page.waitForFunction(`window.location.href.startsWith("${baseUrl}")`);
        }
    } catch (error) {
        // первым делом проверьте наличие дырок до baseUrl, если падает в этом месте
        console.error('AUTHENTICATION_FAILED', error);
        throw error;
    }
};

export function authBrowser() {
    beforeAll(async () => {
        const {login, password, passportUrl, multikHost, authCheckUrl} = config;
        const baseUrl = `${multikHost}${authCheckUrl}`;

        await authenticate(page, baseUrl, passportUrl, login, password);
    });
}

export function catchErrors(ignoreMessages = []) {
    let errors, pageErrors, errorMessages;

    beforeEach(async () => {
        pageErrors = [];
        errors = [];
        errorMessages = [];

        await jestPuppeteer.resetPage();
        page.on('pageerror', (error) => pageErrors.push(error));
        page.on('error', (error) => errors.push(error));

        page.on('console', (message) => {
            const [messageType, text] = [message.type(), message.text()];
            if (messageType === 'error' && !ignoreMessages.includes(text)) {
                errorMessages.push(text);
            }
        });
    });

    afterEach(() => {
        expect({
            pageErrors,
            errors,
            errorMessages,
        }).toEqual({
            errors: [],
            pageErrors: [],
            errorMessages: [],
        });
    });
}

beforeEach(async () => {
    page.setDefaultTimeout(config.selectorTimeout);
    page.setDefaultNavigationTimeout(config.navigationTimeout);
});

afterEach(async () => {
    await page.evaluate(() => localStorage.clear());
});
