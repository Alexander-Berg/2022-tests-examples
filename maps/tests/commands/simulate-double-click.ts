import {wrapAsyncCommand} from '../lib/commands-utils';

interface SimulateClickOptions {
    x: number;
    y: number;
    selector?: string;
    // Текстовое описание того, куда надо сделать клик.
    // Необходимо для того чтобы тесты, с использованием данной команды, могли быть выполнены ассессорами.
    description: string;
}

async function simulateDoubleClick(this: WebdriverIO.Browser, {x, y, selector = 'body'}: SimulateClickOptions) {
    await this.moveToObject(selector, Math.round(x), Math.round(y));
    await this.doubleClick(selector);
}

export default wrapAsyncCommand(simulateDoubleClick);
