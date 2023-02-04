package actions

import (
	"testing"

	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

func TestService_Do_Ok(t *testing.T) {
	log := logrus.New()

	a := NewAction("", log)
	s := New(log)
	s.actions = map[pbAction.Action]IAction{pbAction.Action_UNDRAIN: a}

	action, state := s.State()
	require.Equal(t, pbAction.Action_UNKNOWN, action)
	require.Equal(t, pbActionState.State_UNKNOWN, state)

	require.NoError(t, s.Do(pbAction.Action_UNDRAIN))
	action, state = s.State()

	require.Equal(t, pbAction.Action_UNDRAIN, action)
	require.Equal(t, pbActionState.State_ACCEPTED, state)
}

func TestService_Do_Error(t *testing.T) {
	log := logrus.New()

	tests := map[string]struct {
		Action pbAction.Action
		State  pbActionState.State
		Error  error
	}{
		"already-running": {
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_IN_PROGRESS,
			Error:  ErrAlreadyRunning,
		},
		"unknown": {
			Error: ErrUnknownAction,
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			s := New(log)
			s.setActionState(tc.Action, tc.State)

			require.Equal(t, tc.Error, s.Do(pbAction.Action_WAIT_FOR_CHECK_STATUS))
		})
	}
}

func TestService_State(t *testing.T) {
	log := logrus.New()

	s := New(log)

	s.action = pbAction.Action_DRAIN
	s.state = pbActionState.State_IN_PROGRESS
	action, state := s.State()
	require.Equal(t, pbAction.Action_DRAIN, action)
	require.Equal(t, pbActionState.State_IN_PROGRESS, state)
}
