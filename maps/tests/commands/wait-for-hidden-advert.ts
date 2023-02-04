import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

async function waitForHiddenAdvert(this: WebdriverIO.Browser, parentSelector: string): Promise<void> {
    await this.waitForLoadedAdvert();
    await this.waitForVisible(parentSelector);
    await this.waitForHidden(`${parentSelector} ${cssSelectors.advertMock.view}`);
}

export default wrapAsyncCommand(waitForHiddenAdvert);
