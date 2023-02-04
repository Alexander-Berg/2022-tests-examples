import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';
import {getUrl, GetUrlOptions, Application} from '../lib/func/url';
import {usersData, UserId} from '../common/users';
import {UiTheme} from 'types/theme';

interface OpenPageOptions extends GetUrlOptions {
    userId?: UserId;
    readySelector?: string;
    ignoreMapReady?: boolean;
    localStorage?: {
        key: string;
        value: unknown;
    };
}

async function openPage(this: WebdriverIO.Browser, pathname: string, options: OpenPageOptions = {}): Promise<void> {
    const application: Application = options.application || 'maps';
    await this.setMeta('application', application);

    const {ignoreMapReady, ...urlOptions} = options;
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    const theme = (['chrome-dark', 'iphone-dark'].includes(this.executionContext.browserId)
        ? 'dark'
        : 'light') as UiTheme;

    const optionsWithTheme = {theme, ...urlOptions};
    const url = getUrl(pathname, optionsWithTheme);

    if (options.userId) {
        await openUrlWithLogin(this, url, options.userId);
    } else {
        await this.url(url);
    }

    // Ожидаем приложение, чтобы появился доступ к localStorage.
    await this.waitForExist('body');
    await this.clearLocalStorage().catch(() => {});
    if (options.localStorage) {
        await this.setLocalStorage(options.localStorage.key, options.localStorage.value);
        await this.refresh();
    }
    await this.checkStandError();

    // Оставляем delay, чтобы работали события transitionend, animationend.
    await this.addStyles(`*, *::before, *::after {
        animation-duration: 0s !important;
        animation-delay: 1ms !important;
        animation-fill-mode: forwards !important;

        transition-property: none;
        transition-duration: 0s !important;
        transition-delay: 1ms !important;
    }`);

    await this.waitForExist(options.readySelector || cssSelectors.appReady);
    if (!ignoreMapReady) {
        await this.waitForExist(cssSelectors.mapReady);

        if (options.enableVector) {
            await this.waitForExist(cssSelectors.vectorReady);
        }
    }
}

const loginSelectors = cssSelectors.passportLoginDomik;

async function openUrlWithLogin(browser: WebdriverIO.Browser, url: string, userId: UserId): Promise<void> {
    const {login, password} = usersData[userId];
    const passportUrl = 'https://passport.yandex.ru/auth?retpath=' + encodeURIComponent(url) + '&from=maps';

    if (['chrome', 'chrome-dark', 'iphone', 'iphone-dark', 'google'].includes(browser.executionContext.browserId)) {
        const timestamp = Math.round(new Date().getTime() / 1000);
        const html = `
            <html>
                <form method="POST" action="${passportUrl}">
                    <input name="login" value="${login}">
                    <input name="passwd" value="${password}">
                    <input type="checkbox" name="twoweeks" value="no">
                    <input type="hidden" name="timestamp" value="${timestamp}">
                    <button type="submit">Login</button>
                </form>
            <html>
        `;
        const base64 = Buffer.from(html).toString('base64');

        await browser.url('data:text/html;base64,' + base64);
        const form = await browser.$('form');
        await form.waitForExist();
        const button = await browser.$('button[type=submit]');
        await button.click();
    } else {
        await browser.url(passportUrl);
        await browser.waitForVisible(loginSelectors.page);
        const beforeLoginUrl = getLastUrlElement(await browser.getUrl());
        if (beforeLoginUrl === 'welcome' || beforeLoginUrl === 'list') {
            await browser.waitAndClick(loginSelectors.anotherLink);
        }
        await browser.waitForVisible(loginSelectors.form);
        await browser.waitForVisible(loginSelectors.loginInput);
        await browser.setValueToInput(loginSelectors.loginInput, login);
        await browser.waitAndClick(loginSelectors.submitButton);
        await browser.waitForVisible(loginSelectors.passwordInput);
        await browser.setValueToInput(loginSelectors.passwordInput, password);
        await browser.waitAndClick(loginSelectors.submitButton);
        await browser.waitUntil(async () => {
            const lastElement = getLastUrlElement(await browser.getUrl());
            return lastElement !== 'welcome';
        });
        const afterLoginUrl = getLastUrlElement(await browser.getUrl());
        if (afterLoginUrl === 'social') {
            await browser.waitAndClick(loginSelectors.ignoreSocialButton);
        }
        if (afterLoginUrl === 'phone') {
            await browser.waitAndClick(loginSelectors.ignorePhoneButton);
        }
        if (afterLoginUrl === 'avatar') {
            await browser.waitAndClick(loginSelectors.ignoreAvatarButton);
        }
        if (afterLoginUrl === 'email') {
            await browser.waitAndClick(loginSelectors.ignoreEmailButton);
        }
    }

    await browser.setMeta('url', url);
}

function getLastUrlElement(url: string): string {
    const parts = new URL(url).pathname.replace(/\/$/, '').split('/');
    return parts[parts.length - 1];
}

export default wrapAsyncCommand(openPage);
export {OpenPageOptions};
