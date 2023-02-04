import {wrapAsyncCommand} from '../lib/commands-utils';

interface MovePointerParams {
    selector?: string;
    x: number;
    y: number;
}

async function movePointer(this: WebdriverIO.Browser, params: MovePointerParams): Promise<void> {
    const selector = params.selector || 'body';
    // дважды выполняем команду, чтобы сгенерировалось mousemove событие
    await this.moveToObject(selector, params.x, params.y);
    await this.moveToObject(selector, params.x, params.y);
}

export default wrapAsyncCommand(movePointer);
