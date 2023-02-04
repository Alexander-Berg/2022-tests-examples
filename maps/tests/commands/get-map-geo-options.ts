import {wrapAsyncCommand} from '../lib/commands-utils';

interface MapGeoOptions {
    center: Point;
    zoom: number;
}

async function getMapGeoOptions(this: WebdriverIO.Browser): Promise<MapGeoOptions> {
    const mapState = await this.execute(function (): MapGeoOptions {
        return {
            zoom: window.yandex_map?.zoom,
            center: window.yandex_map?.center
        };
    });

    if (!mapState.zoom || !mapState.center) {
        throw new Error('Не удалось получить значения координат центра карты или зума из инстанса карты.');
    }

    return {
        center: mapState.center,
        zoom: Number(mapState.zoom)
    };
}

export default wrapAsyncCommand(getMapGeoOptions);
