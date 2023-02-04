import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from '../../tests/commands/open-page';

async function openMapWidgetPage(
    this: WebdriverIO.Browser,
    pathname: string,
    options: OpenPageOptions = {}
): Promise<void> {
    await this.openPage(pathname, {...options, application: 'map-widget'});
    await this.waitForVisible(cssSelectors.mapWidget.app);
}

export default wrapAsyncCommand(openMapWidgetPage);
export {OpenPageOptions};
