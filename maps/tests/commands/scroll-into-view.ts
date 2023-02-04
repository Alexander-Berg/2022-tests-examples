import {wrapAsyncCommand} from '../lib/commands-utils';

interface HermioneScrollIntoViewOptions extends ScrollOptions {
    // По умолчанию "start"
    vertical?: ScrollLogicalPosition;
    // По умолчанию "nearest"
    horizontal?: ScrollLogicalPosition;
}

async function scrollIntoView(
    this: WebdriverIO.Browser,
    selector: string,
    options: HermioneScrollIntoViewOptions = {vertical: 'start', horizontal: 'nearest'}
): Promise<void> {
    const element = await this.$(selector);
    await element.waitForExist();
    await element.scrollIntoView({
        behavior: options.behavior,
        block: options.vertical,
        inline: options.horizontal
    });
}

export default wrapAsyncCommand(scrollIntoView);
