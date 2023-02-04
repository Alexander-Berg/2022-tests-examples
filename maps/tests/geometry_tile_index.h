#pragma once

#include "maps/factory/libs/tileindex/impl/tree_editable.h"
#include <maps/factory/libs/tileindex/release.h>

#include <maps/libs/geolib/include/prepared_polygon.h>

#include <boost/range/adaptor/transformed.hpp>
#include <boost/range/numeric.hpp>

namespace maps {
namespace tileindex {
namespace impl {

class GeometryTileIndex {
public:
    GeometryTileIndex(TArrayRef<const Release> releases)
    {
        Zoom maxZoom = 0;
        for (const auto& release: releases) {
            const auto zooms = release.zooms();
            if (!zooms.empty()) {
                maxZoom = std::max(maxZoom, zooms.back());
            }
        }
        zoomGeoms_.resize(maxZoom + 1u);

        static const geolib3::MultiPolygon2 empty;
        for (const auto& rel: releases) {
            issues_.push_back(rel.issue());
            for (Zoom zoom = 0; zoom <= maxZoom; ++zoom) {
                zoomGeoms_[zoom].push_back(
                    std::make_unique<geolib3::PreparedPolygon2>(
                        rel.hasZoom(zoom) ? rel.geometry(zoom) : empty));
            }
        }
    }

    TArrayRef<const Issue> issues() const { return issues_; }

    boost::optional<RootIdx> resolveRootIndex(
        RootIdx rootStamp,
        const Tile& tile) const
    {
        if (tile.zoom() >= zoomGeoms_.size()) {
            return boost::none;
        }
        using geolib3::spatialRelation;
        const auto geoTile = MercatorProjection{}(tile);
        const auto& geoms = zoomGeoms_[tile.zoom()];
        for (ptrdiff_t rootIdx = rootStamp; rootIdx >= 0; --rootIdx) {
            if (spatialRelation(*geoms[rootIdx], geoTile,
                geolib3::Disjoint)) {
                continue;
            }
            return rootIdx;
        }
        return boost::none;
    }

    boost::optional<Issue> resolveIssue(const Tile& tile) const
    {
        if (issues_.empty()) {
            return boost::none;
        }
        if (const auto rootIdx
            = resolveRootIndex(issues_.size() - 1u, tile)) {
            return issues_[*rootIdx];
        }
        return boost::none;
    }

private:
    std::vector<Issue> issues_{};
    std::vector<std::vector<std::unique_ptr<geolib3::PreparedPolygon2>>>
        zoomGeoms_{};
};

} // namespace impl
} // namespace tileindex
} // namespace maps
