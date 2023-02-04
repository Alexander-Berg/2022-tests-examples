package tests

import (
	"testing"
	"time"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/porto"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

func TestShutdownPorto_Execute_ContainerKillSuccess(t *testing.T) {
	m := porto.NewFakePorto()
	e := &env.Env{
		Porto: m,
		L:     l,
		Sleep: func(duration time.Duration) {},
	}
	runPorto := tasks.NewShutdownPorto("test", tasks.NewSimpleCond(&pb.Condition{}))
	m.On("Status", mock.Anything).Return(&porto.Status{Running: true, Exist: true, Etag: "etag"}, nil).Once()
	m.On("Disable", mock.Anything).Return(nil).Once()
	m.On("Kill", mock.Anything, mock.Anything).Return(nil).Once()
	m.On("WaitDead", mock.Anything, mock.Anything, mock.Anything).Return(true, nil).Once()
	m.On("Destroy", mock.Anything).Return(nil).Once()
	m.On("Status", mock.Anything).Return(&porto.Status{Running: false, Exist: false, Etag: ""}, nil)
	pl := changelog.New()
	err := runPorto.Execute(e, pl)
	if err != nil {
		t.Error(err.Error())
		t.Fail()
	}
	assert.Equal(t, pl.Events, []changelog.Event{{
		Event:   "porto.disable",
		Message: "test",
	}, {
		Event:   "porto.kill",
		Message: "test",
	}, {
		Event:   "porto.destroy",
		Message: "test",
	}})
}
