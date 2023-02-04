package tests

import (
	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/file"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"errors"
	"fmt"
	"github.com/stretchr/testify/assert"
	"syscall"
	"testing"
)

func TestRemove_Execute_RemoveNotExist(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Remove").Return(syscall.ENOENT)
	paths := []string{f1.Path}
	chl := changelog.New()

	remove := tasks.NewRemove(paths, tasks.NewSimpleCond(&pb.Condition{}))
	err := remove.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, make([]changelog.Event, 0), chl.Events)
}

func TestRemove_Execute_RemoveExisted(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Remove").Return(nil)
	paths := []string{f1.Path}
	chl := changelog.New()

	remove := tasks.NewRemove(paths, tasks.NewSimpleCond(&pb.Condition{}))
	err := remove.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, []changelog.Event{{
		Event:   "file.removed",
		Message: f1.Path,
	}}, chl.Events)
}

func TestRemove_Execute_RemoveFail(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Remove").Return(errors.New("failed to remove"))
	paths := []string{f1.Path}
	chl := changelog.New()

	remove := tasks.NewRemove(paths, tasks.NewSimpleCond(&pb.Condition{}))
	err := remove.Execute(envFromFile(f), chl)

	assert.Error(t, err)
	assert.Equal(t, make([]changelog.Event, 0), chl.Events)
}

func TestRemove_Execute_Plan(t *testing.T) {
	paths := []string{f1.Path}
	p := make([]map[string]string, 0)

	remove := tasks.NewRemove(paths, tasks.NewSimpleCond(&pb.Condition{}))
	p = remove.Plan(p)

	assert.Equal(t, fmt.Sprintf("paths: {%s}", f1.Path), p[0]["file.removed"])
}

func TestRemove_Execute_PlanNoFiles(t *testing.T) {
	paths := make([]string, 0)
	p := make([]map[string]string, 0)

	remove := tasks.NewRemove(paths, tasks.NewSimpleCond(&pb.Condition{}))
	p = remove.Plan(p)

	assert.Equal(t, 0, len(p))
}
