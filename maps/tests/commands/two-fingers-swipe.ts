import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

const DISTANCE = 200;
const DISTANCE_BETWEEN_FINGERS = 100;

async function twoFingersSwipe(this: WebdriverIO.Browser, direction: 'up' | 'down'): Promise<void> {
    await this.perform(async () => {
        const map = await this.$(cssSelectors.map.container);

        await this.performActions([
            {
                type: 'pointer',
                id: 'finger1',
                parameters: {pointerType: 'touch'},
                actions: [
                    {type: 'pointerMove', duration: 0, origin: map, x: DISTANCE_BETWEEN_FINGERS / 2, y: 0},
                    {type: 'pointerDown', button: 0},
                    {
                        type: 'pointerMove',
                        duration: 150,
                        origin: 'pointer',
                        x: 0,
                        y: direction === 'up' ? -DISTANCE : DISTANCE
                    },
                    {type: 'pointerUp', button: 0}
                ]
            },
            {
                type: 'pointer',
                id: 'finger2',
                parameters: {pointerType: 'touch'},
                actions: [
                    {type: 'pointerMove', duration: 0, origin: map, x: -DISTANCE_BETWEEN_FINGERS / 2, y: 0},
                    {type: 'pointerDown', button: 0},
                    {
                        type: 'pointerMove',
                        duration: 150,
                        origin: 'pointer',
                        x: 0,
                        y: direction === 'up' ? -DISTANCE : DISTANCE
                    },
                    {type: 'pointerUp', button: 0}
                ]
            }
        ]);
        await this.releaseActions();
    }, `Совершить свайп карты двумя пальцами ${direction === 'up' ? 'вверх' : 'вниз'}`);
}

export default wrapAsyncCommand(twoFingersSwipe);
