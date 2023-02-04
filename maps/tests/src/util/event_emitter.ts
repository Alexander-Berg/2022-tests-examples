import {EventEmitter} from '../../../src/vector_render_engine/util/event_emitter';

/**
 * Creates Promise that resolves by the first event from emitter or rejects by timeout.
 */
export async function promisifyEventEmitter<ArgsT extends any[] = []>(
    emitter: EventEmitter<ArgsT>,
    timeout: number = 100
): Promise<ArgsT> {
    return new Promise((resolve, reject) => {
        setTimeout(reject, timeout);

        emitter.addListener((...args: ArgsT) => {
            resolve(args);
        })
    });
}
