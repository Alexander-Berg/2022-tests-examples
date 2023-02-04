import {wrapAsyncCommand} from '../lib/commands-utils';

async function tap(this: WebdriverIO.Browser, position: Point, tapType: 'tap' | 'longtap' = 'tap') {
    return this.dragPointer({
        startPosition: position,
        endPosition: position,
        duration: tapType === 'longtap' ? 1000 : 10,
        description: `'Тапнуть в точке ${position.join(',')}'`
    });
}

export default wrapAsyncCommand(tap);
