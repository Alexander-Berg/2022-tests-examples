package tests

import (
	"testing"
	"time"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/slot"

	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/porto"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func TestRunPorto_Execute_ContainerIsRunning(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		Porto: m,
		L:     l,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{}
	runPorto := tasks.NewRunPorto("test", "etag", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "", nil)
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil)
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 0)
	assert.Equal(t, st.TotalRespawnCount, int32(0))
	assert.Equal(t, st.RespawnCount, int32(0))
}

func TestRunPorto_Execute_ContainerIsRunning_RespawnedBefore(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		Porto: m,
		L:     l,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{TotalRespawnCount: 1, RespawnCount: 1}
	runPorto := tasks.NewRunPorto("test", "etag", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "1", nil)
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil)
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 0)
	assert.Equal(t, st.TotalRespawnCount, int32(1))
	assert.Equal(t, st.RespawnCount, int32(0))
}

func TestRunPorto_Execute_ContainerIsRunning_SingleRespawn(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		Porto: m,
		L:     l,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{}
	runPorto := tasks.NewRunPorto("test", "etag", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "1", nil)
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil)
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 0)
	assert.Equal(t, st.TotalRespawnCount, int32(1))
	assert.Equal(t, st.RespawnCount, int32(1))
}

func TestRunPorto_Execute_StartContainer(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		L:     l,
		Porto: m,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{}
	runPorto := tasks.NewRunPorto("test", "etag", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "1", nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: false, Exist: false, Etag: ""}, nil).Twice()
	m.On("Destroy", mock.Anything).Return(nil).Once()
	m.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil).Twice()
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 1)
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "porto.start",
		Message: "test",
	}})
	assert.Equal(t, st.TotalRespawnCount, int32(1))
	assert.Equal(t, st.RespawnCount, int32(1))
}

func TestRunPorto_Execute_ContainerCanNotStart(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		L:     l,
		Porto: m,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{}
	runPorto := tasks.NewRunPorto("test", "etag", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "1", nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: false, Exist: false, Etag: ""}, nil)
	m.On("Destroy", mock.Anything).Return(nil).Once()
	m.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	assert.True(t, err != nil)
	assert.Equal(t, len(pl.Events), 3)
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "porto.start",
		Message: "test",
	}, {
		Event:   "porto.start",
		Message: "test",
	}, {
		Event:   "porto.start",
		Message: "test",
	}})
	assert.Equal(t, st.TotalRespawnCount, int32(1))
	assert.Equal(t, st.RespawnCount, int32(1))
}

func TestRunPorto_Execute_OutdatedContainer(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		L:     l,
		Porto: m,
		Sleep: func(duration time.Duration) {},
	}
	props := &pb.PortoProperties{}
	st := &slot.Status{}
	runPorto := tasks.NewRunPorto("test", "etag2", props, tasks.NewSimpleCond(&pb.Condition{}), st)
	m.On("Get", mock.Anything, mock.Anything).Return("respawn_count", "1", nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: false, Exist: false, Etag: "etag"}, nil).Twice()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag2"}, nil).Twice()
	m.On("WaitDead", mock.Anything, mock.Anything, mock.Anything).Return(true, nil).Once()
	m.On("Kill", mock.Anything, mock.Anything).Return(nil).Once()
	m.On("Destroy", mock.Anything).Return(nil).Once()
	m.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(nil).Once()
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, len(pl.Events), 3)
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "porto.kill",
		Message: "test",
	}, {
		Event:   "porto.destroy",
		Message: "test",
	}, {
		Event:   "porto.start",
		Message: "test",
	}})
	assert.Equal(t, st.TotalRespawnCount, int32(1))
	assert.Equal(t, st.RespawnCount, int32(1))
}
