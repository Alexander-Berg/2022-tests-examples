import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

async function waitForVisibleAdvert(this: WebdriverIO.Browser, parentSelector: string): Promise<void> {
    await this.waitForLoadedAdvert();
    await this.waitForViewportVisibility(
        `${parentSelector} ${cssSelectors.advertMock.view}`,
        'atLeastPartiallyVisible'
    );
}

export default wrapAsyncCommand(waitForVisibleAdvert);
