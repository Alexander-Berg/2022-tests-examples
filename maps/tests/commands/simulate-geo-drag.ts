import getPositionFromPoint from '../lib/func/get-position-from-point';
import {wrapAsyncCommand} from '../lib/commands-utils';

type Options = {
    /**
     * Координаты объекта, который нужно перетащить.
     * Для организаций их можно получить через шаринг в карточке.
     * Во всех остальных случаях нужно получать либо из данных, либо через "Что здесь".
     */
    start: Point;
    /**
     * Координаты, в которые нужно перетащить объект.
     */
    end: Point;
    fireMoveBeforeDrag?: boolean;
    floorPixelCoordinates?: boolean;
    duration?: number;
};

async function simulateGeoDrag(
    this: WebdriverIO.Browser,
    {start, end, fireMoveBeforeDrag, floorPixelCoordinates, ...options}: Options
): Promise<void> {
    const centerPosition = await this.getMapCenter();
    const {center, zoom} = await this.getMapGeoOptions();
    const startPosition = getPositionFromPoint({point: start, zoom, center, centerPosition, floorPixelCoordinates});
    const endPosition = getPositionFromPoint({point: end, zoom, center, centerPosition, floorPixelCoordinates});

    if (fireMoveBeforeDrag) {
        await this.movePointer({x: startPosition[0], y: startPosition[1]});
    }

    await this.dragPointer({
        startPosition,
        endPosition,
        description: `Драгнуть карту из ${startPosition.join(',')} в ${endPosition.join(',')}`,
        ...options
    });
}

export default wrapAsyncCommand(simulateGeoDrag);
