import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';
import isMobileBrowser from '../lib/func/is-mobile-browser';

async function submitSearch(this: WebdriverIO.Browser, query: string): Promise<void> {
    await this.setValueToInput(cssSelectors.search.input, query);
    if (isMobileBrowser(this)) {
        await this.keys('Enter');
    } else {
        await this.waitAndClick(cssSelectors.search.searchButton);
        await this.moveToObject(cssSelectors.search.input);
    }
}

export default wrapAsyncCommand(submitSearch);
