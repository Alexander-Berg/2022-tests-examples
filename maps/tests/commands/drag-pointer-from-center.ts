import {wrapAsyncCommand} from '../lib/commands-utils';
import {ToParams, CommonDragParams} from './drag-pointer';

async function dragPointerFromCenter(
    this: WebdriverIO.Browser,
    params: ToParams & CommonDragParams & {excludeSidebarHeight?: boolean}
): Promise<void> {
    await this.dragPointer({
        startPosition: await this.getMapCenter({excludeSidebarHeight: params.excludeSidebarHeight ?? true}),
        ...params
    });
}

export default wrapAsyncCommand(dragPointerFromCenter);
