#pragma once

#include <library/cpp/actors/testlib/test_runtime.h>

#include <solomon/libs/cpp/logging/logging.h>

namespace NSolomon {

/**
 * An actor runtime implementation for testing purposes.
 *
 * Runs actor system in a simulated environment, provides introspection for debug purposes, can skip simulation time.
 * Note: you should use `NActors::TActivationContext::Now()` in your actors because simulated time may differ
 * from system time.
 */
class TTestActorRuntime: public NActors::TTestActorRuntimeBase {
protected:
    /**
     * Create an uninitialized instance of runtime. This constructor is protected to make initialization
     * story explicit. Use `CreateRaw` or `CreateInited` functions instead.
     *
     * @param nodeCount         number of execution nodes, i.e. simulated servers. Useful for testing interconnect
     *                          functionality.
     * @param useRealThreads    spawn real threads instead of running actors in the main thread. May be useful to debug
     *                          concurrency issues with tsan.
     * @param enableScheduling  enable scheduling for all events. See `SetScheduledEventFilterWildcard`.
     */
    explicit TTestActorRuntime(ui32 nodeCount = 1, bool useRealThreads = false, bool enableScheduling = true);

public:
    /**
     * Create an uninitialized instance of runtime. Don't forget to call `Initialize` or use `CreateInited`.
     */
    static THolder<TTestActorRuntime> CreateRaw(
        ui32 nodeCount = 1, bool useRealThreads = false, bool enableScheduling = true);

    /**
     * Create an initialized instance of runtime.
     */
    static THolder<TTestActorRuntime> CreateInited(
        ui32 nodeCount = 1, bool useRealThreads = false, bool enableScheduling = true);

public:
    /**
     * Register a new actor within an actor system.
     *
     * If you're registering a bootstrapped actor, use `WaitForBootstrap` after calling this funciton.
     */
    using TTestActorRuntimeBase::Register;

    NActors::TActorId Register(
        THolder<NActors::IActor> actor,
        ui32 nodeIndex = 0,
        ui32 poolId = 0,
        NActors::TMailboxType::EType mailboxType = NActors::TMailboxType::Simple,
        ui64 revolvingCounter = 0,
        const NActors::TActorId& parentid = NActors::TActorId());

    NActors::TActorId Register(
            std::unique_ptr<NActors::IActor> actor,
            ui32 nodeIndex = 0,
            ui32 poolId = 0,
            NActors::TMailboxType::EType mailboxType = NActors::TMailboxType::Simple,
            ui64 revolvingCounter = 0,
            const NActors::TActorId& parentid = NActors::TActorId());

    /**
     * Send message to an actor.
     *
     * If you intent to get a response, create a listening actor using the `AllocateEdgeActor` method and use
     * the returned id as a sender. Then, wait for response using one of `GrabEdgeEvent` methods.
     */
    using TTestActorRuntimeBase::Send;

    void Send(NActors::TActorId recipient, THolder<NActors::IEventBase> ev, ui32 flags = 0, ui64 cookie = 0) {
        TTestActorRuntimeBase::Send(new NActors::IEventHandle{recipient, {}, ev.Release(), flags, cookie});
    }

    void Send(NActors::TActorId recipient, NActors::TActorId sender, THolder<NActors::IEventBase> ev, ui32 flags = 0, ui64 cookie = 0) {
        TTestActorRuntimeBase::Send(new NActors::IEventHandle{recipient, sender, ev.Release(), flags, cookie});
    }

    void Send(NActors::TActorId recipient, std::unique_ptr<NActors::IEventBase> ev, ui32 flags = 0, ui64 cookie = 0) {
        TTestActorRuntimeBase::Send(new NActors::IEventHandle{recipient, {}, ev.release(), flags, cookie});
    }

    void Send(NActors::TActorId recipient, NActors::TActorId sender, std::unique_ptr<NActors::IEventBase> ev, ui32 flags = 0, ui64 cookie = 0) {
        TTestActorRuntimeBase::Send(new NActors::IEventHandle{recipient, sender, ev.release(), flags, cookie});
    }

    /**
     * Process a single bootstrap event. Call this after registering a bootstrapped actor.
     */
    void WaitForBootstrap();

    /**
     * Disable filtering for schedule events. By default,
     */
    void SetScheduledEventFilterWildcard();

protected:
    void InitNodeImpl(TNodeDataBase* node, size_t idx) override;
};

} // namespace NSolomon
