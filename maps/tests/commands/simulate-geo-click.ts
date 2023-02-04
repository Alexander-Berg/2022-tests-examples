import getPositionFromPoint from '../lib/func/get-position-from-point';
import {wrapAsyncCommand} from '../lib/commands-utils';

interface SimulateGeoClickOptions {
    /**
     * Координаты точки, куда надо кликнуть. Для организаций их можно получить через шаринг в карточке.
     * Во всех остальных случаях нужно получать либо из данных, либо через "Что здесь".
     */
    point: Point;
    selector?: string;
    fireMoveBeforeClick?: boolean;
    floorPixelCoordinates?: boolean;
    doubleClick?: boolean;
    /**
     * Текстовое описание того, куда надо сделать клик.
     * Необходимо для того чтобы тесты, с использованием данной команды, могли быть выполнены ассессорами.
     */
    description: string;
}

async function simulateGeoClick(
    this: WebdriverIO.Browser,
    {point, selector = 'body', fireMoveBeforeClick, doubleClick, floorPixelCoordinates}: SimulateGeoClickOptions
): Promise<void> {
    const {center, zoom} = await this.getMapGeoOptions();
    const centerPosition = await this.getMapCenter();
    const [x, y] = getPositionFromPoint({point, zoom, center, centerPosition, floorPixelCoordinates});

    if (fireMoveBeforeClick) {
        await this.movePointer({selector, x, y});
    }

    if (doubleClick) {
        await this.simulateDoubleClick({selector, x, y, description: ''});
    } else {
        await this.simulateClick({x, y, selector, description: ''});
    }
}

export default wrapAsyncCommand(simulateGeoClick);
