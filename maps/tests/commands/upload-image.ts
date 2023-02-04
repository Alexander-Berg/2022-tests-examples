import * as path from 'path';
import {wrapAsyncCommand} from '../../tests/lib/commands-utils';

type Filename = '1080x1920' | '300x300' | '300x600' | '300x450' | '100x65' | 'moscow';
type Ext = 'jpg' | 'jpeg' | 'png' | 'txt';

async function uploadImage(
    this: WebdriverIO.Browser,
    inputSelector: string,
    filename: Filename,
    ext: Ext = 'jpg'
): Promise<void> {
    const filepath = path.join(__dirname, '../images/' + filename + '.' + ext);
    const remoteFilename = await this.uploadFile(filepath);
    const removeStyles = await this.addStyles(`${inputSelector} {display: block !important}`);

    const input = await this.$(inputSelector);
    await input.setValue(remoteFilename);
    await removeStyles();
}

export default wrapAsyncCommand(uploadImage);
