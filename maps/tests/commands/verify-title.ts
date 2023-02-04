import {expect} from 'chai';
import {wrapAsyncCommand} from '../lib/commands-utils';

async function verifyTitle(this: WebdriverIO.Browser, title: string): Promise<void> {
    await this.waitUntil(async () => {
        const text = await this.getTitle();
        expect(text).to.be.equal(title, 'Title не совпадает');
        return true;
    });
}

export default wrapAsyncCommand(verifyTitle);
