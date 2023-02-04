import {wrapAsyncCommand} from '../lib/commands-utils';

type VisibilitySide = 'top' | 'right' | 'bottom' | 'left';
type Visibility = Record<VisibilitySide, boolean>;

async function getViewportVisibility(this: WebdriverIO.Browser, selector: string): Promise<Visibility> {
    const viewportSize = await this.getViewportSize();
    const element = await this.$(selector);
    const location = await element.getLocation();
    const elementSize = await element.getSize();
    const windowScroll = await getWindowScroll(this);
    return getSidesVisibility(viewportSize, location, elementSize, windowScroll);
}

function getSidesVisibility(
    {width: viewportWidth, height: viewportHeight}: WebdriverIO.Size,
    {x, y}: WebdriverIO.Position,
    {width: elementWidth, height: elementHeight}: WebdriverIO.Size,
    {x: windowScrollX, y: windowScrollY}: WebdriverIO.Position
): Visibility {
    const [[left, right], [top, bottom]] = [
        {
            coordinate: x - windowScrollX,
            elementSize: elementWidth,
            viewportSize: viewportWidth
        },
        {
            coordinate: y - windowScrollY,
            elementSize: elementHeight,
            viewportSize: viewportHeight
        }
    ].map(({coordinate, elementSize, viewportSize}) =>
        [coordinate, coordinate + elementSize].map((coord) => coord >= 0 && coord <= viewportSize)
    );
    return {top, bottom, left, right};
}

async function getWindowScroll(browser: WebdriverIO.Browser): Promise<WebdriverIO.Position> {
    const result = await browser.execute(() => {
        return {y: window.pageYOffset, x: window.pageXOffset};
    });
    return result;
}

export default wrapAsyncCommand(getViewportVisibility);
