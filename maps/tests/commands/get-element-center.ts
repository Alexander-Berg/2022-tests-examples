import {wrapAsyncCommand} from '../lib/commands-utils';

async function getElementCenter(this: WebdriverIO.Browser, selector: string): Promise<Point> {
    const elements = await this.$$(selector);
    const positions = await Promise.all(elements.map((element) => element.getLocation()));
    const sizes = await Promise.all(elements.map((element) => element.getSize()));
    const centers = positions.map<Point>((position, index) => {
        const size = sizes[index] || {width: 0, height: 0};
        return [position.x + size.width / 2, position.y + size.height / 2];
    });
    // Если получится сделать overload функции - добавить возможность возвращать centers
    return centers[0];
}

export default wrapAsyncCommand(getElementCenter);
