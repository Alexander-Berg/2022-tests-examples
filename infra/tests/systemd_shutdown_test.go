package tests

import (
	"errors"
	"testing"
	"time"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/systemd"
	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/specutil"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func prepareShutdownSystemdMocks(s *systemd.Fake) {
	s.On("Disable", mock.Anything).Return(nil)
	s.On("Stop", mock.Anything).Return(nil)
	s.On("Stop", mock.Anything, mock.Anything).Return(nil)
}

func TestShutdownSystemd_Execute_ServiceStopped(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateExited,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Log(pl.Fmt("", "", ""))
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 0)
}

func TestShutdownSystemd_Execute_ServiceActiveEnabled(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Log(pl.Fmt("", "", ""))
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		}, {
			Event:   "systemd.stop",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_ServiceEnabled(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateExited,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Log(pl.Fmt("", "", ""))
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_ServiceActive(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Log(pl.Fmt("", "", ""))
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.stop",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_CanNotStop(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.Errorf(t, err, "failed to execute [disable, stop] states 3 times")
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.stop",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_CanNotDisable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateExited,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.Errorf(t, err, "failed to execute [disable, stop] states 3 times")
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_CanNotStopDisable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareShutdownSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.Errorf(t, err, "failed to execute [disable, stop] states 3 times")
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		}, {
			Event:   "systemd.stop",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_ServiceFailedToStop(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("Disable", mock.Anything).Return(nil)
	s.On("Stop", mock.Anything, mock.Anything).Return(errors.New("test fail"))
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.Errorf(t, err, "test fail")
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		}, {
			Event:   "systemd.stop",
			Message: "test.service",
		},
	})
}

func TestShutdownSystemd_Execute_ServiceFailedToDisable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewShutdownSystemService("test", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("Disable", mock.Anything).Return(errors.New("test fail"))
	s.On("Stop", mock.Anything, mock.Anything).Return(nil)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.Errorf(t, err, "test fail")
	assert.Equal(t, pl.Events, []changelog.Event{
		{
			Event:   "systemd.disable",
			Message: "test.service",
		},
	})
}
