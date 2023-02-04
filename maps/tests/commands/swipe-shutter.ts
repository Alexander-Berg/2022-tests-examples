import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

interface SwipeOptions {
    parentSelector: string;
    distance: number;
}

async function swipeShutter(
    this: WebdriverIO.Browser,
    direction: 'up' | 'down',
    options: Partial<SwipeOptions> = {}
): Promise<void> {
    const parentSelector = options.parentSelector || '.body';
    const distance = options.distance || 200;
    const selector = `${parentSelector} ${cssSelectors.expandButton}`;
    await this.waitForVisible(selector);
    await this.dragPointer({
        selector,
        deltaY: direction === 'up' ? -distance : distance,
        forceTouch: true,
        description: `Свайпнуть шторку ${direction === 'up' ? 'вверх' : 'вниз'}`
    });
}

export default wrapAsyncCommand(swipeShutter);
