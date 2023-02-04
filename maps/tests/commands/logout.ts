import {wrapAsyncCommand} from '../lib/commands-utils';

const logoutUrl = 'https://passport.yandex.ru/passport?mode=logout&yu=';

async function logout(this: WebdriverIO.Browser): Promise<void> {
    if (['chrome', 'iphone', 'google'].includes(this.executionContext.browserId)) {
        await this.deleteCookies();
        return;
    }
    const [cookie] = await this.getCookies('yandexuid');
    if (cookie && cookie.value) {
        await this.url(logoutUrl + cookie.value);
    }
}

export default wrapAsyncCommand(logout);
