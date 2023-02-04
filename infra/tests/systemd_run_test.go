package tests

import (
	"errors"
	"fmt"
	"testing"
	"time"

	"a.yandex-team.ru/infra/hostctl/internal/units/specutil"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks/systemdstates"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/systemd"
	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
)

func prepareSystemdMocks(s *systemd.Fake) {
	s.On("ReloadDaemon").Return(nil)
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Stop", mock.Anything, mock.Anything).Return(nil)
}

func TestRunSystemd_Execute_ServiceIsRunning(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 0)
}

func TestRunSystemd_Execute_NeedDaemonReload(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}})
}

func TestRunSystemd_Execute_ServiceOutdated(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.restart",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_ServiceStopped(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.start",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_ServiceStoppedNotLoaded(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateNotFound,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_EnableNtp(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("ntp", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.enable",
		Message: "ntp.service",
	}})
}

func TestRunSystemd_Execute_ReloadDaemonStartEnable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}, {
		Event:   "systemd.enable",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_ReloadDaemonReloadEnable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         true,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.reload",
		Message: "test.service",
	}, {
		Event:   "systemd.enable",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_Enable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateRunning,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
	}, nil).Once()
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.enable",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_CanNotStart(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	prepareSystemdMocks(s)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "failed to restart/reload/start systemd service after 3 attempts")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_FailedToEnable(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Enable", mock.Anything).Return(errors.New("test error"))
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}, {
		Event:   "systemd.enable",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_FailedToStart(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(errors.New("test error"))
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.start",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_FailedToRestart(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewRestartSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(errors.New("test error"))
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.restart",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_FailedToReload(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(errors.New("test error"))
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateActive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         true,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}, {
		Event:   "systemd.reload",
		Message: "test.service",
	}})
}

func TestRunSystemd_Execute_FailedToDaemonReload(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(errors.New("test error"))
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		SubState:         systemd.SubStateDead,
		ActiveState:      systemd.ActiveStateInactive,
		UnitFileState:    systemd.UnitFileStateDisabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: true,
		Outdated:         false,
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "systemd.daemon-reload",
		Message: "",
	}})
}

func TestRunSystemd_Execute_FailedToGetStatus(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	retryPolicy := specutil.RetryPolicyFromUpdatePolicyOrDefaults(nil)
	runSystemd := tasks.NewReloadSystemService("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}), retryPolicy)
	s.On("ReloadDaemon").Return(nil)
	s.On("Restart", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Enable", mock.Anything).Return(nil)
	s.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	s.On("Reload", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	var empty *systemd.UnitStatus
	s.On("Status", mock.Anything, mock.Anything).Return(empty, errors.New("test error"))
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "test error")
	assert.Equal(t, len(pl.Events), 0)
}

func TestRunSystemd_Execute_JobExecutionToLong(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	runSystemd := tasks.NewRestartOneShotJob("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}))
	prepareSystemdMocks(s)
	now := time.Now()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateActivating,
		SubState:         systemd.SubStateStart,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now.Add(-20 * time.Minute),
			ExecMainStatus: "0",
		},
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "job 'test.service' not finished")
	assert.Equal(t, pl.Events, []changelog.Event{})
}

func TestRunSystemd_Execute_NotZeroExitStatus(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	runSystemd := tasks.NewRestartOneShotJob("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}))
	prepareSystemdMocks(s)
	now := time.Now()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateInactive,
		SubState:         systemd.SubStateExited,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now.Add(-(systemdstates.RestartThrottleTimeout + time.Minute)),
			Stop:           now.Add(-(systemdstates.RestartThrottleTimeout + time.Minute)),
			ExecMainStatus: "1",
		},
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, "failed to run systemd unit: Exited='true' Outdated='false' ExecMainStatus='1' Duration='0s'")
	assert.Equal(t, pl.Events, []changelog.Event{{Event: "systemd.start", Message: "test.service"}})
}

func TestRunSystemd_Execute_NotZeroExitStatus_RestartThrottled(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	runSystemd := tasks.NewRestartOneShotJob("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}))
	prepareSystemdMocks(s)
	now := time.Now()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateInactive,
		SubState:         systemd.SubStateExited,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now,
			Stop:           now,
			ExecMainStatus: "1",
		},
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	assert.EqualError(t, err, fmt.Sprintf("unit exit code = 1, will restart after %s", now.Add(systemdstates.RestartThrottleTimeout).Format("15:04:05")))
	assert.Equal(t, pl.Events, make([]changelog.Event, 0))
}

func TestRunSystemd_Execute_NotZeroExitStatus_RestartNotThrottledOnOutdatedService(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	runSystemd := tasks.NewRestartOneShotJob("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}))
	prepareSystemdMocks(s)
	now := time.Now()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateInactive,
		SubState:         systemd.SubStateExited,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         true,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now,
			Stop:           now,
			ExecMainStatus: "1",
		},
	}, nil).Once()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateActive,
		SubState:         systemd.SubStateExited,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now,
			Stop:           now,
			ExecMainStatus: "0",
		},
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err)
	}
	assert.Equal(t, pl.Events, []changelog.Event{{Event: "systemd.start", Message: "test.service"}})
}

func TestRunSystemd_Execute_ZeroExitStatus(t *testing.T) {
	s := systemd.NewFake()
	e := &env.Env{
		Systemd: s,
		L:       l,
		Sleep:   func(duration time.Duration) {},
		Mode:    env.TestMode,
	}
	runSystemd := tasks.NewRestartOneShotJob("test", "rev1", (*tasks.SimpleCondition)(&pb.Condition{}))
	prepareSystemdMocks(s)
	now := time.Now()
	s.On("Status", mock.Anything, mock.Anything).Return(&systemd.UnitStatus{
		ActiveState:      systemd.ActiveStateInactive,
		SubState:         systemd.SubStateExited,
		UnitFileState:    systemd.UnitFileStateEnabled,
		LoadState:        systemd.LoadStateLoaded,
		NeedDaemonReload: false,
		Outdated:         false,
		Type:             systemd.TypeOneshot,
		ExecMain: &systemd.ExecMain{
			Start:          now,
			Stop:           now,
			ExecMainStatus: "0",
		},
	}, nil)
	pl := changelog.New()
	err := runSystemd.Execute(e, pl)
	if err != nil {
		t.Error(err)
	}
	assert.Equal(t, pl.Events, []changelog.Event{})
}
