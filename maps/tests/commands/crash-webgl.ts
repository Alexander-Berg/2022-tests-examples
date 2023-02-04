import {wrapAsyncCommand} from '../lib/commands-utils';

async function crashWebgl(this: WebdriverIO.Browser): Promise<void> {
    await this.execute(() => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        window.document.querySelector('canvas').getContext('webgl').getExtension('WEBGL_lose_context').loseContext();
    });
}

export default wrapAsyncCommand(crashWebgl);
