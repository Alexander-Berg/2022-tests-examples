import {Scene} from '../../../../src/vector_render_engine/content_provider/base_vector_content_provider/scene';
import {VectorPrimitiveTypes} from '../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {RenderablePolygon} from '../../../../src/vector_render_engine/primitive/polygon/renderable_polygon';
import {TexRenderablePolygon} from '../../../../src/vector_render_engine/primitive/tex_polygon/renderable_tex_polygon';
import {RenderableModel} from '../../../../src/vector_render_engine/primitive/model/renderable_model';
import {RenderablePolyline} from '../../../../src/vector_render_engine/primitive/polyline/renderable_polyline';
import {RenderableLabel} from '../../../../src/vector_render_engine/primitive/label/renderable_label';
import {RenderableIcon} from '../../../../src/vector_render_engine/primitive/icon/renderable_icon';
import {PrimitiveArrayStorage} from '../../../../src/vector_render_engine/render/primitive/primitive_array_storage';
import {DisappearingPrimitiveArrayStorage} from '../../../../src/vector_render_engine/render/primitive/disappearing_primitive_array_storage';

export class SceneStub implements Scene {
    readonly [VectorPrimitiveTypes.OPAQUE_POLYGON] = new PrimitiveArrayStorage<RenderablePolygon>();
    readonly [VectorPrimitiveTypes.TRANSPARENT_POLYGON] = new PrimitiveArrayStorage<RenderablePolygon>();
    readonly [VectorPrimitiveTypes.TEXTURED_POLYGON] = new PrimitiveArrayStorage<TexRenderablePolygon>();
    readonly [VectorPrimitiveTypes.MESH] = new PrimitiveArrayStorage<RenderableModel>();
    readonly [VectorPrimitiveTypes.POLYLINE] = new PrimitiveArrayStorage<RenderablePolyline>();
    readonly [VectorPrimitiveTypes.POINT_LABEL] = new DisappearingPrimitiveArrayStorage<RenderableLabel>();
    readonly [VectorPrimitiveTypes.CURVED_LABEL] = new DisappearingPrimitiveArrayStorage<RenderableLabel>();
    readonly [VectorPrimitiveTypes.ICON] = new DisappearingPrimitiveArrayStorage<RenderableIcon>();
};
