#include "mock_system_focus_manager.h"

#include <yandex/maps/runtime/assert.h>

namespace yandex::maps::navi::audio_focus::tests {

namespace {
MockSystemFocusManager::StateType toStateType(SystemFocusManager::ShortSoundType type)
{
    return type == SystemFocusManager::ShortSoundType::Annotation
        ? MockSystemFocusManager::StateType::Annotation
        : MockSystemFocusManager::StateType::Alert;
}
}  // anonymous namespace

MockSystemFocusManager::MockSystemFocusManager()
    : requiredStates_(createStates(State::Deactivated))
    , systemStates_(createStates(State::Activated))
{
}

bool MockSystemFocusManager::requestShortSoundFocus(ShortSoundType type)
{
    return requestFocus(toStateType(type));
}

bool MockSystemFocusManager::requestPlayFocus()
{
    return requestFocus(StateType::Play);
}

bool MockSystemFocusManager::requestPlayAndRecordFocus()
{
    return requestFocus(StateType::PlayAndRecord);
}

SystemFocusManager::State MockSystemFocusManager::shortSoundFocusState(ShortSoundType type) const
{
    return currentState(toStateType(type));
}

SystemFocusManager::State MockSystemFocusManager::playFocusState() const
{
    return currentState(StateType::Play);
}

SystemFocusManager::State MockSystemFocusManager::playAndRecordFocusState() const
{
    return currentState(StateType::PlayAndRecord);
}

void MockSystemFocusManager::abandonShortSoundFocus()
{
    requiredStates_[StateType::Alert] = State::Deactivated;
    requiredStates_[StateType::Annotation] = State::Deactivated;
}

void MockSystemFocusManager::abandonPlayFocus()
{
    requiredStates_[StateType::Play] = State::Deactivated;
}

void MockSystemFocusManager::abandonPlayAndRecordFocus()
{
    requiredStates_[StateType::PlayAndRecord] = State::Deactivated;
}

void MockSystemFocusManager::setListener(SystemFocusListener* listener)
{
    listener_ = listener;
}

void MockSystemFocusManager::switchMySpinMode()
{
}

void MockSystemFocusManager::setSystemState(StateType stateType, State newState)
{
    if (setSystemStateWithoutListenerNotification(stateType, newState) && listener_) {
        listener_->onSystemFocusChanged();
    }
}

void MockSystemFocusManager::setSystemStates(const States& states)
{
    bool changed = false;
    for (const auto& state : states) {
        changed = setSystemStateWithoutListenerNotification(state.first, state.second) || changed;
    }

    if (listener_ && changed) {
        listener_->onSystemFocusChanged();
    }
}

bool MockSystemFocusManager::setSystemStateWithoutListenerNotification(StateType stateType, State newState)
{
    const auto oldState = currentState(stateType);

    systemStates_[stateType] = newState;
    if (!activatedOrSuspended(systemStates_[stateType]) && activatedOrSuspended(requiredStates_[stateType])) {
        requiredStates_[stateType] = systemStates_[stateType];
    }

    const auto updatedState = currentState(stateType);
    return oldState != updatedState;
}

void MockSystemFocusManager::resetSystemStates()
{
    setSystemStates(createStates(State::Activated));
}

MockSystemFocusManager::States MockSystemFocusManager::createStates(State defaultState)
{
    return {
        { StateType::Alert, defaultState },
        { StateType::Annotation, defaultState },
        { StateType::Play, defaultState },
        { StateType::PlayAndRecord, defaultState },
    };
}

SystemFocusManager::State MockSystemFocusManager::currentState(StateType stateType) const
{
    const auto system = systemStates_.at(stateType);
    const auto required = requiredStates_.at(stateType);

    static const auto states = { State::Deactivated, State::Lost, State::Suspended, State::Activated };
    for (const auto state : states) {
        if (state == required || state == system) {
            return state;
        }
    }

    ASSERT(false);
    return State::Deactivated;
}

bool MockSystemFocusManager::requestFocus(StateType stateType)
{
    if (activatedOrSuspended(systemStates_[stateType])) {
        requiredStates_[stateType] = State::Activated;
    }

    return currentState(stateType) == State::Activated;
}

bool MockSystemFocusManager::activatedOrSuspended(State state) const
{
    return state == State::Activated || state == State::Suspended;
}

}  // namespace yandex
