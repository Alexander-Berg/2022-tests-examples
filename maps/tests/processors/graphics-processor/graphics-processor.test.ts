import * as assert from 'assert';
import graphicsProcessor from '../../../server/processors/graphics-processor';
import GraphicsScheme from '../../../server/processors/types/graphics-scheme';

const obj1 = {
    id: '0a1b8e79',
    type: 'Object',
    x: 3,
    y: 1008,
    z: 0,
    content: {
        $ref: '/common/templates/[svgImage]',
        $params: {
            resource: {
                $ref: '/parameters/images/bg-z4.svg'
            }
        }
    }
};

const obj2 = {
    id: '695fb574',
    type: 'Object',
    x: 0,
    y: 0,
    z: 1018,
    content: {
        $ref: '/common/templates/[linkLine]',
        $params: {
            color: {
                $ref: '/parameters/colors/line-16'
            },
            linkHollowOverlayLineOpacity: 0,
            linkOutterStrokeWidth: 12,
            linkStrokeWidth: 8,
            linkClosedDashArray: [4, 4],
            linkClosedOverlayStrokeWidth: 4,
            x1: 760,
            y1: 1850,
            x2: 760,
            y2: 1900
        }
    }
};

const scheme = {
    type: 'Scheme',
    attributes: {
        size: {
            width: 3000,
            height: 3500
        },
        center: {
            x: 1508,
            y: 1433
        },
        scale: {
            initial: 2.13,
            overview: 1,
            detailed: 3.9,
            min: 0.6,
            max: 5.2
        },
        backgroundColor: {
            $ref: '/parameters/colors/background'
        },
        styles: ['light', 'dark'],
        defaultStyle: 'light',
        darkStyle: 'dark'
    },
    objects: {
        type: 'IndexedCollection',
        items: [obj1, obj2]
    }
};

const common = {
    templates: {
        type: 'IndexedCollection',
        items: [
            {
                id: 'svgImage',
                type: 'Template',
                layers: [
                    {
                        id: 'image',
                        type: 'SVGImage',
                        resource: '#{resource}'
                    }
                ]
            },
            {
                id: 'linkLine',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Line',
                        opacity: 1,
                        strokeLineCap: 'butt',
                        strokeWidth: '#{linkOutterStrokeWidth}',
                        strokeColor: {
                            $ref: '/parameters/colors/linkStroke'
                        },
                        y2: '#{y2}',
                        x2: '#{x2}',
                        y1: '#{y1}',
                        x1: '#{x1}',
                        z: 0
                    },
                    {
                        id: 'frontLine',
                        type: 'Line',
                        opacity: 1,
                        strokeLineCap: 'butt',
                        strokeWidth: '#{linkStrokeWidth}',
                        strokeColor: '#{color}',
                        y2: '#{y2}',
                        x2: '#{x2}',
                        y1: '#{y1}',
                        x1: '#{x1}',
                        z: 1
                    },
                    {
                        id: 'hollowOverlayLine',
                        type: 'Line',
                        opacity: '#{linkHollowOverlayLineOpacity}',
                        strokeLineCap: 'butt',
                        strokeWidth: '#{linkClosedOverlayStrokeWidth}',
                        strokeColor: {
                            $ref: '/parameters/colors/linkStroke'
                        },
                        y2: '#{y2}',
                        x2: '#{x2}',
                        y1: '#{y1}',
                        x1: '#{x1}',
                        z: 2
                    }
                ]
            }
        ]
    }
};

const parameters = {
    colors: {
        background: 'ffffff',
        linkStroke: 'ffffff',
        multiserviceStationBackground: 'ffffff',
        'line-1': 'da2128',
        'line-16': 'ffc61a'
    },
    images: {
        'bg-z4.svg': {
            file: 'bg-z4.svg'
        }
    }
};

describe('graphicsProcessor', () => {
    it('should not fail on empty parameters object', () => {
        const expected = {
            graphics: {
                '0a1b8e79': [
                    {
                        id: 'image',
                        type: 'SVGImage',
                        resource: 'unknown',
                        x: 3,
                        y: 1008
                    }
                ]
            },
            presets: {}
        };
        assert.deepEqual(
            graphicsProcessor(({
                scheme: {...scheme, objects: {type: 'Indexed Collection', items: [obj1]}},
                common,
                parameters: {colors: {}, images: {}}
            } as unknown) as GraphicsScheme),
            expected
        );
    });

    it('should not fail on empty templates array', () => {
        assert.deepEqual(
            graphicsProcessor(({
                scheme,
                common: {templates: {type: 'Templates', items: []}},
                parameters
            } as unknown) as GraphicsScheme),
            {graphics: {}, presets: {}}
        );
    });

    it('should work on a simple case', () => {
        const expected = {
            graphics: {
                '0a1b8e79': [
                    {
                        id: 'image',
                        type: 'SVGImage',
                        resource: 'bg-z4.svg',
                        x: 3,
                        y: 1008
                    }
                ]
            },
            presets: {}
        };
        assert.deepEqual(
            graphicsProcessor(({
                scheme: {...scheme, objects: {type: 'Indexed Collection', items: [obj1]}},
                common,
                parameters
            } as unknown) as GraphicsScheme),
            expected
        );
    });

    it('should work for multiple objects', () => {
        const expected = {
            graphics: {
                '0a1b8e79': [
                    {
                        id: 'image',
                        type: 'SVGImage',
                        resource: 'bg-z4.svg',
                        x: 3,
                        y: 1008
                    }
                ],
                '695fb574': [
                    {
                        id: 'backLine',
                        type: 'Line',
                        opacity: 1,
                        strokeLinecap: 'butt',
                        strokeWidth: 12,
                        stroke: '#ffffff',
                        y2: 1900,
                        x2: 760,
                        y1: 1850,
                        x1: 760
                    },
                    {
                        id: 'frontLine',
                        type: 'Line',
                        opacity: 1,
                        strokeLinecap: 'butt',
                        strokeWidth: 8,
                        stroke: '#ffc61a',
                        y2: 1900,
                        x2: 760,
                        y1: 1850,
                        x1: 760
                    },
                    {
                        id: 'hollowOverlayLine',
                        type: 'Line',
                        opacity: 0,
                        strokeLinecap: 'butt',
                        strokeWidth: 4,
                        stroke: '#ffffff',
                        y2: 1900,
                        x2: 760,
                        y1: 1850,
                        x1: 760
                    }
                ]
            },
            presets: {}
        };
        assert.deepEqual(
            graphicsProcessor(({
                scheme: {...scheme, objects: {type: 'Indexed Collection', items: [obj1, obj2]}},
                common,
                parameters
            } as unknown) as GraphicsScheme),
        expected
        );
    });
});
