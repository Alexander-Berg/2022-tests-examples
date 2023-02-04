#include "test_functions.h"

#include <infra/pod_agent/libs/behaviour/bt/nodes/base/test/test_functions.h>
#include <infra/pod_agent/libs/pod_agent/object_meta/test_lib/test_functions.h>
#include <infra/pod_agent/libs/pod_agent/status_and_ticker_holder/status_and_ticker_holder.h>

#include <util/string/cast.h>

namespace NInfra::NPodAgent::NObjectTargetTestLib  {

TUpdateHolder::TBoxTarget CreateBoxTargetSimple(
    const TString& id
    , const TString& hash
    , ui32 refs
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TBoxTarget(
        NObjectMetaTestLib::CreateBoxMetaSimple(
            id
            , refs
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetBoxTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TBoxTarget CreateBoxTargetWithDependence(
    const TString& id
    , const TString& hash
    , const TVector<TString>& rootfsLayerRefs
    , const TVector<TString>& staticResourceRefs
    , const TVector<TString>& volumeRefs
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TBoxTarget(
        NObjectMetaTestLib::CreateBoxMetaWithDependence(
            id
            , rootfsLayerRefs
            , staticResourceRefs
            , volumeRefs
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetBoxTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TLayerTarget CreateLayerTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 seed
    , bool removeSourceFileAfterImport
    , bool correctTreeId
) {
    return TUpdateHolder::TLayerTarget(
        NObjectMetaTestLib::CreateLayerMetaSimple(
            id
            , downloadHash
            , seed
            , removeSourceFileAfterImport
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetLayerTreeId(downloadHash) : ("random_prefix_" + downloadHash))
    );
}

TUpdateHolder::TLayerTarget CreateCacheLayerTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    , ui32 seed
    , bool removeSourceFileAfterImport
    , bool correctTreeId
) {
    return TUpdateHolder::TLayerTarget(
        NObjectMetaTestLib::CreateCacheLayerMetaSimple(
            id
            , downloadHash
            , revision
            , seed
            , removeSourceFileAfterImport
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetLayerTreeId(downloadHash) : ("random_prefix_" + downloadHash))
    );
}

TUpdateHolder::TStaticResourceTarget CreateStaticResourceTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TStaticResourceTarget(
        NObjectMetaTestLib::CreateStaticResourceMetaSimple(
            id
            , downloadHash
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetStaticResourceTreeId(downloadHash) : ("random_prefix_" + downloadHash))
    );
}

TUpdateHolder::TStaticResourceTarget CreateCacheStaticResourceTargetSimple(
    const TString& id
    , const TString& downloadHash
    , ui32 revision
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TStaticResourceTarget(
        NObjectMetaTestLib::CreateCacheStaticResourceMetaSimple(
            id
            , downloadHash
            , revision
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetStaticResourceTreeId(downloadHash) : ("random_prefix_" + downloadHash))
    );
}

TUpdateHolder::TVolumeTarget CreatePersistentVolumeTargetSimple(
    const TString& id
    , const TString& hash
    , ui32 refs
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TVolumeTarget(
        NObjectMetaTestLib::CreateVolumeMetaSimple(
            id
            , refs
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetVolumeTreeId(id) : ("random_prefix_" + id))
        , hash
        , true /* isPersistent */
    );
}

TUpdateHolder::TVolumeTarget CreatePersistentVolumeTargetWithDependence(
    const TString& id
    , const TString& hash
    , const TVector<TString>& layerRefs
    , const TVector<TString>& staticResourceRefs
    , ui32 seed
    , bool correctTreeId
) {
    return TUpdateHolder::TVolumeTarget(
        NObjectMetaTestLib::CreateVolumeMetaWithDependence(
            id
            , layerRefs
            , staticResourceRefs
            , seed
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetVolumeTreeId(id) : ("random_prefix_" + id))
        , hash
        , true /* isPersistent */
    );
}

TUpdateHolder::TVolumeTarget CreateNonPersistentVolumeTargetSimple(
    const TString& id
    , const TString& hash
) {
    return TUpdateHolder::TVolumeTarget(
        NObjectMetaTestLib::CreateVolumeMetaSimple(
            id
            , 0
            , 1
        )
        , GetEmptyTree("random_prefix_" + id)
        , hash
        , false /* isPersistent */
    );
}

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetSimple(
    const TString& id
    , const TString& hash
    , ui32 readinessType
    , ui32 boxId
    , ui32 seed
    , bool withContainers
    , bool correctTreeId
) {
    return TUpdateHolder::TWorkloadTarget(
        NObjectMetaTestLib::CreateWorkloadMetaSimple(
            id
            , readinessType
            , boxId
            , seed
            , withContainers
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetWorkloadTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithoutBox(
    const TString& id
    , const TString& hash
    , ui32 readinessType
    , ui32 seed
    , bool withContainers
    , bool correctTreeId
) {
    return TUpdateHolder::TWorkloadTarget(
        NObjectMetaTestLib::CreateWorkloadMetaWithoutBox(
            id
            , readinessType
            , seed
            , withContainers
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetWorkloadTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithoutBox(
    const TString& id
    , const TString& hash
    , ui32 readinessType
    , ui32 boxId
    , ui32 seed
    , bool withContainers
    , bool correctTreeId
) {
    return TUpdateHolder::TWorkloadTarget(
        NObjectMetaTestLib::CreateWorkloadMetaSimple(
            id
            , readinessType
            , boxId
            , seed
            , withContainers
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetWorkloadTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithSpecificHookTypes(
    const TString& id
    , const TString& hash
    , ui32 boxId
    , ui32 seed
    , ui32 readinessType
    , ui32 livenessType
    , ui32 stopType
    , ui32 destroyType
    , bool correctTreeId
) {
    return TUpdateHolder::TWorkloadTarget(
        NObjectMetaTestLib::CreateWorkloadMetaWithSpecificHookTypes(
            id
            , boxId
            , seed
            , readinessType
            , livenessType
            , stopType
            , destroyType
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetWorkloadTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

TUpdateHolder::TWorkloadTarget CreateWorkloadTargetWithDependence(
    const TString& id
    , const TString& hash
    , const TString& boxRef
    , ui32 readinessType
    , ui32 seed
    , bool withContainers
    , bool correctTreeId
) {
    return TUpdateHolder::TWorkloadTarget(
        NObjectMetaTestLib::CreateWorkloadMetaWithDependence(
            id
            , boxRef
            , readinessType
            , seed
            , withContainers
        )
        , GetEmptyTree(correctTreeId ? TStatusNTickerHolder::GetWorkloadTreeId(id) : ("random_prefix_" + id))
        , hash
    );
}

} // namespace NInfra::NPodAgent::NObjectTargetTestLib
