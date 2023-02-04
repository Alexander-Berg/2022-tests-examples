import Vector2, * as vector2 from '../../src/vector_render_engine/math/vector2';
import {NormalizedDisplayCoordinates} from '../../src/vector_render_engine/util/display_coordinates';

class CanvasRenderersUnequal extends Error {
    constructor(details: string) {
        super(`Renderer outputs are not identical: ${details}`);
    }
}

/**
 * Helper method for canvas renderers comparison. Its callback parameter corresponds to mocha's callback for async
 * test, it gets error passed if the renderer outputs are not equal.
 */
export function compareRendererOutputs(
    renderer: CanvasRenderer,
    imageRenderer: CanvasImageRenderer,
    callback: (error?: Error) => void
): void {
    imageRenderer.initialization.then(() => {
        const data1 = renderer.render().context.getImageData(0, 0, renderer.width, renderer.height).data;
        const data2 = imageRenderer.render().context.getImageData(0, 0, imageRenderer.width, imageRenderer.height).data;

        if (data1.length !== data2.length) {
            return callback(new CanvasRenderersUnequal('sizes differ'));
        }

        for (let i = 0; i < data1.length; i++) {
            if (data1[i] !== data2[i]) {
                return callback(new CanvasRenderersUnequal('contents differ'));
            }
        }

        return callback();
    });

}

/**
 * Some visual functionality (like text layouting) is easier to test by comparing with a reference image that is
 * verified manually. That is what this base class is for. It contains some utility methods that simplify
 * filling canvas with something meaningful and provide a png data url of the output.
 */
export default class CanvasRenderer {
    readonly canvas: HTMLCanvasElement;
    readonly context: CanvasRenderingContext2D;
    readonly width: number;
    readonly height: number;

    constructor(canvas: HTMLCanvasElement) {
        this.canvas = canvas;
        this.context = this.canvas.getContext('2d')!;
        this.width = canvas.width;
        this.height = canvas.height;
    }

    /**
     * Draws everything on the canvas.
     */
    render(): this {
        this.onRender();

        return this;
    }

    /**
     * @param type Preferred mime type of the output.
     * @return Base64 image data url of the canvas's current state.
     */
    getImageDataUrl(type: string = 'image/png'): string {
        this.render();

        return this.canvas.toDataURL(type);
    }

    /**
     * @returns ImageData of the canvas's current state.
     */
    getImageData(): ImageData {
        return this.context.getImageData(0, 0, this.width, this.height);
    }

    /**
     * Derived classes have to implement their specific drawings in this method.
     */
    protected onRender(): void {}

    /**
     * @return Canvas coordinates that corresponds to provided  normalized coordinates.
     */
    protected toCanvasCoordinates(normalizedCoordinated: NormalizedDisplayCoordinates): Vector2 {
        return vector2.create(
            (this.width / 2) * (1 + normalizedCoordinated.x),
            (this.height / 2) * (1 - normalizedCoordinated.y)
        );
    }

    /**
     * Draws a polygon on the canvas filled by default color (opaque black).
     */
    protected _drawPolygon(points: Vector2[], color: string = '#000000'): void {
        this.context.beginPath();
        this.context.moveTo(points[0].x, points[0].y);

        for (const point of points) {
            this.context.lineTo(point.x, point.y);
        }

        this.context.fillStyle = color;
        this.context.fill();
    }

    /**
     * Draws a polyline on the canvas with default color (opaque black).
     */
    protected _drawPolyline(points: Vector2[], color: string = '#000000'): void {
        this.context.beginPath();
        this.context.moveTo(points[0].x, points[0].y);

        for (const point of points) {
            this.context.lineTo(point.x, point.y);
        }

        this.context.strokeStyle = color;
        this.context.stroke();
    }

    /**
     * Draws an image on the canvas.
     */
    protected _drawImage(image: HTMLImageElement, dst: Vector2 = vector2.create(0, 0)): void {
        this.context.drawImage(image, dst.x, dst.y);
    }
}

export class FreshCanvasRenderer extends CanvasRenderer {
    constructor(width: number = 100, height: number = 100) {
        super(FreshCanvasRenderer._createCanvas(width, height));
    }

    private static _createCanvas(width: number, height: number): HTMLCanvasElement {
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        return canvas;
    }
}

/**
 * Renderer whose single responsibility is to draw an image into canvas, the image is provided as base64 data url.
 * To make sure the image is on the canvas and everything is ready one should use the initialization property,
 * that is because of the async nature of image loading in some browsers.
 */
export class CanvasImageRenderer extends FreshCanvasRenderer {
    width: number;
    height: number;
    readonly image: HTMLImageElement;
    readonly initialization: Promise<void>;

    constructor(base64dataUrl: string) {
        const image = new Image();
        const initialization = new Promise<any>((resolve) => image.onload = () => {
            this.width = this.canvas.width = this.image.width;
            this.height = this.canvas.height = this.image.height;
            resolve();
        });

        image.src = base64dataUrl;

        super(image.width, image.height);

        this.image = image;
        this.initialization = initialization;
    }

    protected onRender(): void {
        this._drawImage(this.image);
    }
}

/**
 * Renders image from its RGBA data of each pixel, see the ImageData API for details.
 */
export class CanvasImageDataRenderer extends FreshCanvasRenderer {
    /**
     * @param width The width of the image.
     * @param height The height of the image.
     * @param data Content of the image in RGBA format, each component is represented as a separate value, clamped
     *      to [0, 255] interval, thus the size of the data array should be exactly equal to width * height * 4.
     */
    constructor(width: number, height: number, data: ArrayBuffer) {
        super(width, height);

        this._data = new ImageData(new Uint8ClampedArray(data), width, height);
    }

    protected onRender(): void {
        this.context.putImageData(this._data, 0, 0);
    }

    private readonly _data: ImageData;
}
