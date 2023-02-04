import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from '../../tests/commands/open-page';

async function openMetroPage(
    this: WebdriverIO.Browser,
    pathname: string,
    options: OpenPageOptions = {}
): Promise<void> {
    await this.openPage(pathname, {...options, application: 'metro', ignoreMapReady: true, instantZoom: true});
    await this.waitForVisible(cssSelectors.metro.schemeDecorations);
}

export default wrapAsyncCommand(openMetroPage);
export {OpenPageOptions};
