#include "actor_runtime.h"

namespace NSolomon {

TTestActorRuntime::TTestActorRuntime(ui32 nodeCount, bool useRealThreads, bool enableScheduling)
    : TTestActorRuntimeBase{nodeCount, useRealThreads}
{
    if (enableScheduling) {
        SetScheduledEventFilterWildcard();
    }
}

THolder<TTestActorRuntime> TTestActorRuntime::CreateRaw(ui32 nodeCount, bool useRealThreads, bool enableScheduling) {
    return THolder<TTestActorRuntime>(new TTestActorRuntime{nodeCount, useRealThreads, enableScheduling});
}

THolder<TTestActorRuntime> TTestActorRuntime::CreateInited(ui32 nodeCount, bool useRealThreads, bool enableScheduling) {
    auto runtime = CreateRaw(nodeCount, useRealThreads, enableScheduling);
    runtime->Initialize();
    return runtime;
}

NActors::TActorId TTestActorRuntime::Register(
        THolder<NActors::IActor> actor,
        ui32 nodeIndex,
        ui32 poolId,
        NActors::TMailboxType::EType mailboxType,
        ui64 revolvingCounter,
        const NActors::TActorId& parentid)
{
    return Register(actor.Release(), nodeIndex, poolId, mailboxType, revolvingCounter, parentid);
}

NActors::TActorId TTestActorRuntime::Register(
        std::unique_ptr<NActors::IActor> actor,
        ui32 nodeIndex,
        ui32 poolId,
        NActors::TMailboxType::EType mailboxType,
        ui64 revolvingCounter,
        const NActors::TActorId& parentid)
{
    return Register(actor.release(), nodeIndex, poolId, mailboxType, revolvingCounter, parentid);
}

void TTestActorRuntime::WaitForBootstrap() {
    NActors::TDispatchOptions options;
    options.FinalEvents.emplace_back(NActors::TEvents::TSystem::Bootstrap, 1);

    DispatchEvents(options);
}

void TTestActorRuntime::SetScheduledEventFilterWildcard() {
    SetScheduledEventFilter([=](auto&&, auto&&, auto&&, auto&&) { return false; });
}

void TTestActorRuntime::InitNodeImpl(NActors::TTestActorRuntimeBase::TNodeDataBase* node, size_t idx) {
    node->LogSettings->Append(
        ELogComponent::Logger,
        ELogComponent_MAX,
        ELogComponent_Name
    );

    TTestActorRuntimeBase::InitNodeImpl(node, idx);
}

} // namespace NSolomon
