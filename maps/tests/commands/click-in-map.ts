import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

// Команда должна использоваться для клика в карту. Не надо использовать ее для попадания в определенную точку,
// хотспот или нитку маршрута.
async function clickInMap(this: WebdriverIO.Browser): Promise<void> {
    const element = await this.$(cssSelectors.map.container);
    const {x, y} = await element.getLocation();
    const {height, width} = await element.getSize();

    const points: Point[] = [
        [x + 100, height / 2],
        [width / 2, y + 100],
        [x + width - 100, height / 2],
        [width / 2, y + height - 100]
    ];

    await executeClickInMap(this, points);
}

async function executeClickInMap(browser: WebdriverIO.Browser, points: Point[], pointIndex = 0): Promise<void> {
    const [x, y] = points[pointIndex];

    try {
        await browser.simulateClick({
            x,
            y,
            selector: cssSelectors.map.container,
            description: ''
        });
    } catch (error) {
        const nextPointIndex = pointIndex + 1;

        if (nextPointIndex === points.length) {
            throw error;
        }

        await executeClickInMap(browser, points, nextPointIndex);
    }
}

export default wrapAsyncCommand(clickInMap);
