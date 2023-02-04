#include "test_functions.h"

#include <util/string/cast.h>

namespace NInfra::NPodAgent::NObjectMetaTestLib  {

namespace {

TWorkloadMeta::THookInfo GetWorkloadHookInfo(ui32 type, const TString& containerName) {
    switch (type) {
        case 0:
            return TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo());
        case 1:
            return TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo(containerName));
        case 2:
            return TWorkloadMeta::THookInfo(TWorkloadMeta::THttpGetInfo());
        case 3:
            return TWorkloadMeta::THookInfo(TWorkloadMeta::TTcpCheckInfo());
        case 4:
            return TWorkloadMeta::THookInfo(TWorkloadMeta::TUnixSignalInfo());
    }
    ythrow yexception() << "Cannot parse type: " << ToString(type);
}

} // namespace

TVector<TString> GenerateInitContainerNames(ui32 initSize) {
    TVector<TString> initContainers(initSize);
    for (ui32 i = 0; i < initSize; ++i) {
        initContainers[i] = "init_container_" + ToString(i);
    }

    return initContainers;
}

TVector<TWorkloadMeta::TContainerInfo> GenerateInitContainerInfo(ui32 initSize) {
    TVector<TString> initContainers = GenerateInitContainerNames(initSize);
    TVector<TWorkloadMeta::TContainerInfo> initContainersInfo;
    Transform(initContainers.begin(), initContainers.end(), std::back_inserter(initContainersInfo), [](const TString &containerName) {
        return TWorkloadMeta::TContainerInfo(containerName);
    });

    return initContainersInfo;
}

TBoxMeta CreateBoxMetaSimple(
    const TString& id
    , ui32 refs
    , ui32 seed
) {
    return TBoxMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , refs == 0 ? TVector<TString>() : TVector<TString>({"RootfsLayerRef" + ToString(refs)})
        , refs == 0 ? TVector<TString>() : TVector<TString>({"StaticResourceRef" + ToString(refs)})
        , refs == 0 ? TVector<TString>() : TVector<TString>({"VolumeRef" + ToString(refs)})
        , "box_meta_container_name"
        , GenerateInitContainerNames(seed + 2)
        , "box_specific_type"
    );
}

TBoxMeta CreateBoxMetaWithDependence(
    const TString& id
    , const TVector<TString>& rootfsLayerRefs
    , const TVector<TString>& staticResourceRefs
    , const TVector<TString>& volumeRefs
    , ui32 seed
) {
    return TBoxMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , rootfsLayerRefs
        , staticResourceRefs
        , volumeRefs
        , "box_meta_container_name"
        , GenerateInitContainerNames(seed + 2)
        , "box_specific_type"
    );
}

TLayerMeta CreateLayerMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 seed
    , bool removeSourceFileAfterImport
) {
    return TLayerMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , downloadHash
        , removeSourceFileAfterImport
    );
}

TLayerMeta CreateCacheLayerMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    , ui32 seed
    , bool removeSourceFileAfterImport
) {
    return TLayerMeta(
        id
        , seed        // specTimestamp
        , revision
        , downloadHash
        , removeSourceFileAfterImport
    );
}

TStaticResourceMeta CreateStaticResourceMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 seed
) {
    return TStaticResourceMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , downloadHash
        , seed + 2    // checkPeriodMs
    );
}

TStaticResourceMeta CreateCacheStaticResourceMetaSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    , ui32 seed
) {
    return TStaticResourceMeta(
        id
        , seed        // specTimestamp
        , revision
        , downloadHash
        , seed + 1    // checkPeriodMs
    );
}

TVolumeMeta CreateVolumeMetaSimple(
    const TString& id
    , ui32 refs
    , ui32 seed
) {
    return TVolumeMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , refs == 0 ? TVector<TString>() : TVector<TString>({"LayerRef" + ToString(refs)})
        , refs == 0 ? TVector<TString>() : TVector<TString>({"StaticResourceRef" + ToString(refs)})
    );
}

TVolumeMeta CreateVolumeMetaWithDependence(
    const TString& id
    , const TVector<TString>& layerRefs
    , const TVector<TString>& staticResourceRefs
    , ui32 seed
) {
    return TVolumeMeta(
        id
        , seed        // specTimestamp
        , seed + 1    // revision
        , layerRefs
        , staticResourceRefs
    );
}

TWorkloadMeta CreateWorkloadMetaSimple(
    const TString& id
    , ui32 readinessType
    , ui32 boxId
    , ui32 seed
    , bool withContainers
) {
    return TWorkloadMeta(
        id
        , seed                            // specTimestamp
        , seed + 1                        // revision
        , "boxRef" + ToString(boxId)

        , TWorkloadMeta::TContainerInfo("start_container")
        , GenerateInitContainerInfo(seed + 2)

        , GetWorkloadHookInfo(readinessType, "readiness_container")
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("liveness_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("stop_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("destroy_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
    );
}

TWorkloadMeta CreateWorkloadMetaWithoutBox(
    const TString& id
    , ui32 readinessType
    , ui32 seed
    , bool withContainers
) {
    return TWorkloadMeta(
        id
        , seed                            // specTimestamp
        , seed + 1                        // revision
        , ""

        , TWorkloadMeta::TContainerInfo("start_container")
        , GenerateInitContainerInfo(seed + 2)

        , GetWorkloadHookInfo(readinessType, "readiness_container")
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("liveness_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("stop_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("destroy_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
    );
}

TWorkloadMeta CreateWorkloadMetaWithSpecificHookTypes(
    const TString& id
    , ui32 boxId
    , ui32 seed
    , ui32 readinessType
    , ui32 livenessType
    , ui32 stopType
    , ui32 destroyType
) {
    return TWorkloadMeta(
        id
        , seed                            // specTimestamp
        , seed + 1                        // revision
        , "boxRef" + ToString(boxId)

        , TWorkloadMeta::TContainerInfo("start_container")
        , GenerateInitContainerInfo(seed + 2)

        , GetWorkloadHookInfo(readinessType, "readiness_container")
        , GetWorkloadHookInfo(livenessType, "liveness_container")
        , GetWorkloadHookInfo(stopType, "stop_container")
        , GetWorkloadHookInfo(destroyType, "destroy_container")
    );
}

TWorkloadMeta CreateWorkloadMetaWithDependence(
    const TString& id
    , const TString& boxRef
    , ui32 readinessType
    , ui32 seed
    , bool withContainers
) {
    return TWorkloadMeta(
        id
        , seed                            // specTimestamp
        , seed + 1                        // revision
        , boxRef

        , TWorkloadMeta::TContainerInfo("start_container")
        , GenerateInitContainerInfo(seed + 2)

        , GetWorkloadHookInfo(readinessType, "readiness_container")
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("liveness_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("stop_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
        , withContainers ? TWorkloadMeta::THookInfo(TWorkloadMeta::TContainerInfo("destroy_container")) : TWorkloadMeta::THookInfo(TWorkloadMeta::TEmptyInfo())
    );
}

} // namespace NInfra::NPodAgent::NObjectMetaTestLib
