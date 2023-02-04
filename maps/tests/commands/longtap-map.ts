import {wrapAsyncCommand} from '../lib/commands-utils';

async function longtapMap(this: WebdriverIO.Browser): Promise<void> {
    const value = await this.getMapCenter();
    await this.tap(value, 'longtap');
}

export default wrapAsyncCommand(longtapMap);
