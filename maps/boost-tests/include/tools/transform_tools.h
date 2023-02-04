#pragma once

#include <yandex/maps/renderer/proj/tile.h>

#include <agg_trans_affine.h>

namespace maps { namespace renderer5 { namespace test {

inline agg::trans_affine mtxFromMerc(double x, double y, unsigned int z)
{
    double scale = renderer::proj::pixelPerUnit(z);
    agg::trans_affine mtx;
    mtx.translate(-x, -y);
    mtx.scale(scale);
    return mtx;
}

inline agg::trans_affine mercatorToTile256(
    unsigned int x,
    unsigned int y,
    unsigned int zoom)
{
    auto scale = renderer::proj::pixelPerUnit(zoom);
    auto bbox = renderer::proj::tileToMerc({x, y}, zoom);

    agg::trans_affine mtx;
    mtx.translate(-bbox.x1, -bbox.y1);
    mtx.scale(scale);

    return mtx;
}

} } } // namespace maps::renderer5::test
