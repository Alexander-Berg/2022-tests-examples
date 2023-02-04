#pragma once

#include <library/cpp/actors/core/actor.h>
#include <library/cpp/actors/core/hfunc.h>

namespace NSolomon {

class TTestActor: public NActors::TActor<TTestActor> {
public:
    explicit TTestActor(std::atomic<size_t>* counter, TDuration delay) noexcept
        : TActor<TTestActor>{&TThis::Living}
        , Counter_{counter}
        , Delay_{delay}
    {
    }

private:
    STATEFN(Living) {
        switch (ui32 type = ev->GetTypeRewrite(); type) {
            hFunc(NActors::TEvents::TEvPoison, OnPoison)
            default:
                Y_FAIL("unexpected message type: %d", type);
        }
    }

    STATEFN(Dying) {
        switch (ui32 type = ev->GetTypeRewrite(); type) {
            sFunc(NActors::TEvents::TEvWakeup, Dead)
            default:
                Y_FAIL("unexpected message type: %d", type);
        }
    }

    void OnPoison(NActors::TEvents::TEvPoison::TPtr& ev) {
        ReplyTo_ = ev->Sender;
        if (Delay_) {
            Become(&TThis::Dying);
            Schedule(Delay_, new NActors::TEvents::TEvWakeup);
        } else {
            Dead();
        }
    }

    void Dead() {
        Counter_->fetch_add(1);
        Send(ReplyTo_, new NActors::TEvents::TEvPoisonTaken);
        PassAway();
    }

private:
    std::atomic<size_t>* Counter_;
    NActors::TActorId ReplyTo_;
    TDuration Delay_;
};

} // namespace NSolomon
