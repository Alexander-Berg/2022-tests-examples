/// <reference types='@yandex-int/tests-to-testpalm/out/puppeteer/setup' />

import * as url from 'url';
import * as path from 'path';
import * as qs from 'qs';
import {ClickOptions, ElementHandle, HTTPResponse} from 'puppeteer';
import {User} from '../common/users';
import cssSelectors from '../common/css-selectors';

const STAND_URL = process.env.STAND_URL;
const PASSPORT_URL = 'https://passport.yandex.ru/passport/?test-id=92863&mode=auth';

interface BaseLoginParams extends User {
    url: string;
}

interface PageParams {
    pathname?: string;
    auth?: User;
    query?: Record<string, unknown>;
}

async function openPage(params: PageParams = {}): Promise<void> {
    if (params?.auth) {
        await login({...params.auth, url: PASSPORT_URL});
    }
    if (!STAND_URL) {
        throw new Error('STAND_URL can not be undefined');
    }

    const parsedStandUrl = url.parse(STAND_URL);

    await page.goto(
        url.format({
            ...parsedStandUrl,
            pathname: path.join(parsedStandUrl.pathname || '/', params.pathname || ''),
            search: qs.stringify(params.query, {addQueryPrefix: true})
        }),
        {waitUntil: 'networkidle2'}
    );
}

async function login(baseLoginParams: BaseLoginParams): Promise<void> {
    return page.perform(async () => {
        const ts = Math.round(new Date().getTime() / 1000);
        const form = `
            <form method="POST" action="${PASSPORT_URL}">
                <input name="login" value="${baseLoginParams.login}"/>
                <input name="passwd" value="${baseLoginParams.password}"/>
                <input type="checkbox" name="twoweeks" value="no"/>
                <input type="hidden" name="timestamp" value="${ts}"/>
                <input type="submit"/>
            </form>
        `;

        await page.goto('data:text/html;base64,' + Buffer.from(form).toString('base64'));
        await clickToSelector('input[type=submit]');
        await page.waitForSelector(cssSelectors.auth.profileInfo);
    }, 'Login via Yandex account');
}

async function waitForSelector(selector: string): Promise<ElementHandle<Element> | null> {
    return page.waitForSelector(selector);
}

async function clickToSelector(selector: string, options?: ClickOptions): Promise<void> {
    await waitForSelector(selector);
    await page.click(selector, options);
}

async function clickAndNavigate(selector: string): Promise<HTTPResponse | null> {
    return Promise.all([page.waitForNavigation({waitUntil: 'networkidle0'}), clickToSelector(selector)]).then(
        ([response]) => response
    );
}

export {openPage, clickToSelector, clickAndNavigate, waitForSelector};
