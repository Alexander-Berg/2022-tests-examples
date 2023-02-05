#pragma once

#include "../../system_focus_manager.h"

#include <map>

namespace yandex::maps::navi::audio_focus::tests {

class MockSystemFocusManager : public SwitchableSystemFocusManager {
public:
    enum class StateType { Alert, Annotation, Play, PlayAndRecord };
    using States = std::map<StateType, State>;

    MockSystemFocusManager();

    virtual void init() override {}

    virtual bool requestShortSoundFocus(ShortSoundType type) override;
    virtual bool requestPlayFocus() override;
    virtual bool requestPlayAndRecordFocus() override;

    virtual State shortSoundFocusState(ShortSoundType type) const override;
    virtual State playFocusState() const override;
    virtual State playAndRecordFocusState() const override;

    virtual void abandonShortSoundFocus() override;
    virtual void abandonPlayFocus() override;
    virtual void abandonPlayAndRecordFocus() override;

    virtual void setListener(SystemFocusListener* listener) override;

    virtual void switchMySpinMode() override;

    void setSystemState(StateType stateType, State newState);
    void setSystemStates(const States& states);
    void resetSystemStates();

    static States createStates(State defaultState);

private:
    bool requestFocus(StateType stateType);
    State currentState(StateType stateType) const;

    bool activatedOrSuspended(State state) const;
    bool setSystemStateWithoutListenerNotification(StateType stateType, State newState);

    SystemFocusListener* listener_ = nullptr;

    States requiredStates_;
    States systemStates_;
};

}  // namespace yandex
