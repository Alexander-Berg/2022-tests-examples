#pragma once

#include <infra/pod_agent/libs/pod_agent/object_meta/object_meta.h>

namespace NInfra::NPodAgent::NObjectMetaTestLib  {

// Returns an array of strings: "init_contaier_0", "init_container_1", ...
TVector<TString> GenerateInitContainerNames(ui32 initSize);

// Returns an array of TWorkloadMeta::TContainerInfo with container names: "init_contaier_0", "init_container_1", ...
TVector<TWorkloadMeta::TContainerInfo> GenerateInitContainerInfo(ui32 initSize);

TBoxMeta CreateBoxMetaSimple(
    const TString& id
    // If 0 has no refs, otherwise refs are "RootfsLayerRef<refs>", "StaticResourceRef<refs>", "VolumeRef<refs>"
    , ui32 refs = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
);

TBoxMeta CreateBoxMetaWithDependence(
    const TString& id
    , const TVector<TString>& rootfsLayerRefs = {}
    , const TVector<TString>& staticResourceRefs = {}
    , const TVector<TString>& volumeRefs = {}
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
);

TLayerMeta CreateLayerMetaSimple(
    const TString& id
    , const TString& downloadHash
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    , bool removeSourceFileAfterImport = true
);

TLayerMeta CreateCacheLayerMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    , bool removeSourceFileAfterImport = true
);

TStaticResourceMeta CreateStaticResourceMetaSimple(
    const TString& id
    , const TString& downloadHash
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
);

TStaticResourceMeta CreateCacheStaticResourceMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
);

TVolumeMeta CreateVolumeMetaSimple(
    const TString& id
    // If 0 has no refs, otherwise refs are "LayerRef<refs>"
    , ui32 refs = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
);

TVolumeMeta CreateVolumeMetaWithDependence(
    const TString& id
    , const TVector<TString>& layerRefs = {}
    , const TVector<TString>& staticResourceRefs = {}
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
);

TWorkloadMeta CreateWorkloadMetaSimple(
    const TString& id
    // Almost every test assume that workload has readiness
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // Has box ref "boxRef<boxId>"
    , ui32 boxId = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add all (probably except readiness) workload container names to status
    , bool withContainers = false
);

TWorkloadMeta CreateWorkloadMetaWithoutBox(
    const TString& id
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add all (probably except readiness) workload container names to status
    , bool withContainers = false
);

TWorkloadMeta CreateWorkloadMetaWithSpecificHookTypes(
    const TString& id
    // Has box ref "boxRef<boxId>"
    , ui32 boxId = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // The number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 0
    , ui32 readinessType = 0
    , ui32 livenessType = 0
    , ui32 stopType = 0
    , ui32 destroyType = 0
);

TWorkloadMeta CreateWorkloadMetaWithDependence(
    const TString& id
    , const TString& boxRef = "boxRef"
    // Almost every test assume that workload has readiness
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add all (probably except readiness) workload container names to status
    , bool withContainers = false
);

} // namespace NInfra::NPodAgent::NObjectMetaTestLib

