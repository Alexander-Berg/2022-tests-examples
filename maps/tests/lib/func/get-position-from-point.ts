import {geoToPixels} from 'yandex-geo-utils';

interface Options {
    point: Point;
    center: Point;
    // Позиция центра карты относительно левого верхнего угла окна
    centerPosition: Point;
    zoom: number;

    /**
     * Round pixel coordinates
     */
    floorPixelCoordinates?: boolean;
}

// По географической координате находит позицию в окне браузера
function getPositionFromPoint({point, center, centerPosition, zoom, floorPixelCoordinates = true}: Options): Point {
    const [pixelX, pixelY] = geoToPixels(point, zoom);
    const [centerPixelX, centerPixelY] = geoToPixels(center, zoom);

    const dx = pixelX - centerPixelX;
    const dy = pixelY - centerPixelY;

    const [centerX, centerY] = centerPosition;

    const result = [centerX + dx, centerY + dy] as Point;

    return floorPixelCoordinates ? (result.map(Math.floor) as Point) : result;
}

export default getPositionFromPoint;
