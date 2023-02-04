import {EventTrigger} from '../../../../../src/vector_render_engine/util/event_emitter';
import {LoadManager} from '../../../../../src/vector_render_engine/util/load_manager';

/**
 * Stub LoadManger for tests, it allows to set the result of loading manually.
 */
export class LoadManagerStub extends LoadManager {
    get size(): number {
        return this._loadings.size;
    }
    constructor() {
        super();

        this._requestResolution = new EventTrigger();
        this._loadings = new Set();
    }

    load(_url: string, _priority: number): Promise<ArrayBuffer> {
        const loading = new Promise<ArrayBuffer>((resolve) => {
            const listener = (data: ArrayBuffer) => {
                if (this._loadings.has(loading)) {
                    this._loadings.delete(loading);
                    resolve(data);
                }
                this._requestResolution.removeListener(listener);
            }
            this._requestResolution.addListener(listener);
        });

        this._loadings.add(loading);

        return loading;
    }

    cancel(promise: Promise<ArrayBuffer>): void {
        this._loadings.delete(promise);
    }

    resolveCurrentRequest(data: ArrayBuffer): void {
        this._requestResolution.fire(data);
    }

    private readonly _requestResolution: EventTrigger<[ArrayBuffer]>;
    private readonly _loadings: Set<Promise<ArrayBuffer>>;
}
