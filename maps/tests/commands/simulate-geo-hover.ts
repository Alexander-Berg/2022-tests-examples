import getPositionFromPoint from '../lib/func/get-position-from-point';
import {wrapAsyncCommand} from '../lib/commands-utils';

interface SimulateGeoHoverOptions {
    /**
     * Координаты точки, на которую надо навести мышь. Для организаций их можно получить через шаринг в карточке.
     * Во всех остальных случаях нужно получать либо из данных, либо через "Что здесь".
     */
    point: Point;
    selector?: string;
    /**
     * Текстовое описание того, на какой гео-объект нужно навести курсор.
     * Необходимо для того чтобы тесты, с использованием данной команды, могли быть выполнены ассессорами.
     */
    description: string;
}

async function simulateGeoHover(
    this: WebdriverIO.Browser,
    {point, selector = 'body'}: SimulateGeoHoverOptions
): Promise<void> {
    const {center, zoom} = await this.getMapGeoOptions();
    const centerPosition = await this.getMapCenter();
    const [x, y] = getPositionFromPoint({point, zoom, center, centerPosition});

    await this.movePointer({selector, x, y});
}

export default wrapAsyncCommand(simulateGeoHover);
