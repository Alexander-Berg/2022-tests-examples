#pragma once

#include <infra/pod_agent/libs/pod_agent/update_holder/update_holder.h>

namespace NInfra::NPodAgent::NObjectTargetTestLib  {

TUpdateHolder::TBoxTarget CreateBoxTargetSimple(
    const TString& id
    , const TString& hash = "hash"
    // If 0 has no refs, otherwise refs are "RootfsLayerRef<refs>", "StaticResourceRef<refs>", "VolumeRef<refs>"
    , ui32 refs = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TBoxTarget CreateBoxTargetWithDependence(
    const TString& id
    , const TString& hash = "hash"
    , const TVector<TString>& rootfsLayerRefs = {}
    , const TVector<TString>& staticResourceRefs = {}
    , const TVector<TString>& volumeRefs = {}
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TLayerTarget CreateLayerTargetSimple(
    const TString& id
    , const TString& downloadHash
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    , bool removeSourceFileAfterImport = true
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<downloadHash>"
    , bool correctTreeId = true
);

TUpdateHolder::TLayerTarget CreateCacheLayerTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    , bool removeSourceFileAfterImport = true
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<downloadHash>"
    , bool correctTreeId = true
);

TUpdateHolder::TStaticResourceTarget CreateStaticResourceTargetSimple(
    const TString& id
    , const TString& downloadHash
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<downloadHash>"
    , bool correctTreeId = true
);

TUpdateHolder::TStaticResourceTarget CreateCacheStaticResourceTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<downloadHash>"
    , bool correctTreeId = true
);

TUpdateHolder::TVolumeTarget CreatePersistentVolumeTargetSimple(
    const TString& id
    , const TString& hash = "hash"
    // If 0 has no refs, otherwise refs are "LayerRef<refs>" and "StaticResourceRef<refs>"
    , ui32 refs = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TVolumeTarget CreatePersistentVolumeTargetWithDependence(
    const TString& id
    , const TString& hash = "hash"
    , const TVector<TString>& layerRefs = {}
    , const TVector<TString>& staticResourceRefs = {}
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    , ui32 seed = 1
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TVolumeTarget CreateNonPersistentVolumeTargetSimple(
    const TString& id
    , const TString& hash = "hash"
);

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetSimple(
    const TString& id
    , const TString& hash = "hash"
    // Almost every test assume that workload has readiness
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // Has box ref "boxRef<boxId>"
    , ui32 boxId = 0
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add workload container names to status
    , bool withContainers = false
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithoutBox(
    const TString& id
    , const TString& hash = "hash"
    // Almost every test assume that workload has readiness
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add workload container names to status
    , bool withContainers = false
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithSpecificHookTypes(
    const TString& id
    , const TString& hash = "hash"
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
    , bool correctTreeId = true
);

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithDependence(
    const TString& id
    , const TString& hash = "hash"
    , const TString& boxRef = "boxRef"
    // Almost every test assume that workload has readiness
    // Default THttpGetInfo
    , ui32 readinessType = 2
    // All numeric fields will be generated as seed, seed + 1, seed + 2, ...
    // For backward compatibility the number of init containers is seed + 2,
    // because almost always you only need a number of init containers not a real container names
    , ui32 seed = 1
    // Add workload container names to status
    , bool withContainers = false
    // Tree id will be generated with StatusNTickerHolder, otherwise it will be "random_prefix_<id>"
    , bool correctTreeId = true
);

} // namespace NInfra::NPodAgent::NObjectTargetTestLib
