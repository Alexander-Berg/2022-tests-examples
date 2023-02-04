import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from './open-page';

const DEVICES = {
    iphone_6_7_8: {
        width: 375,
        height: 667
    }
};

interface OpenMobilePageOptions extends OpenPageOptions {
    device: keyof typeof DEVICES;
}

async function openCustomMobilePage(
    this: WebdriverIO.Browser,
    pathname: string,
    {device, ...options}: OpenMobilePageOptions
): Promise<void> {
    if (this.isPhone) {
        throw new Error('Can be run only on a desktop platform. Check the platform and try again.');
    }
    await this.openPageWithCustomViewport(pathname, {...options, ...DEVICES[device], isMobile: true});
}

export default wrapAsyncCommand(openCustomMobilePage);
export {DEVICES};
