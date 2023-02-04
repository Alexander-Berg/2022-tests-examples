package tests

import (
	"errors"
	"fmt"
	"strings"
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/file"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"github.com/stretchr/testify/assert"
)

var (
	f1 = &pb.ManagedFile{
		Path:    "/tmp/a.txt",
		Content: "test",
		User:    "root",
		Group:   "root",
		Mode:    "644",
	}
	f1WithDefault = &pb.ManagedFile{
		Path:    "/tmp/a.txt",
		Content: "test",
		User:    "",
		Group:   "",
		Mode:    "",
	}
	f1AnotherMode = &pb.ManagedFile{
		Path:    "/tmp/a.txt",
		Content: "test",
		User:    "root",
		Group:   "root",
		Mode:    "0644",
	}
	f2 = &pb.ManagedFile{
		Path:    "/tmp/2.txt",
		Content: "test2",
		User:    "vaspahomov",
		Group:   "vaspahomov",
		Mode:    "777",
	}
)

func envFromFile(f file.File) *env.Env {
	l, _ := zap.New(zap.CLIConfig(log.DebugLevel))
	return &env.Env{
		FileBuilder: func(path, content, user, group, mode string) file.File { return f },
		L:           l,
	}
}

func TestManage_Execute_NoChanges(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, changelog.New(), chl)
}

func TestManage_Execute_NoChangesWithDefault(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	f.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1WithDefault}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, changelog.New(), chl)
}
func TestManage_Execute_NoChangesIfModeInAnotherFormat(t *testing.T) {
	f := file.NewFileMock()
	f.On("FromFs").Return(f, nil)
	f.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	f.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1AnotherMode}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, changelog.New(), chl)
}

func TestManage_Execute_ContentChanged(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f1.Content)
	fsFile.On("Content").Return(f2.Content)
	f.On("Mode").Return(f1.Mode)
	fsFile.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	fsFile.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	fsFile.On("Group").Return(f1.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	expected := []changelog.Event{{
		Event:   "file.managed",
		Message: "content: |\n  --- before\n  +++ after\n  @@ -1 +1 @@\n  -test2\n  +test\n",
	}}
	assert.Equal(t, expected, chl.Events)
}

func TestManage_Execute_ModeChanged(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f1.Content)
	fsFile.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	fsFile.On("Mode").Return(f2.Mode)
	f.On("User").Return(f1.User)
	fsFile.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	fsFile.On("Group").Return(f1.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	expected := []changelog.Event{{
		Event:   "file.managed",
		Message: "mode: 777 -> 644\n",
	}}
	assert.Equal(t, expected, chl.Events)
}

func TestManage_Execute_UserChanged(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f1.Content)
	fsFile.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	fsFile.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	fsFile.On("User").Return(f2.User)
	f.On("Group").Return(f1.Group)
	fsFile.On("Group").Return(f1.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	expected := []changelog.Event{{
		Event:   "file.managed",
		Message: "user: vaspahomov -> root\n",
	}}
	assert.Equal(t, expected, chl.Events)
}

func TestManage_Execute_GroupChanged(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f1.Content)
	fsFile.On("Content").Return(f1.Content)
	f.On("Mode").Return(f1.Mode)
	fsFile.On("Mode").Return(f1.Mode)
	f.On("User").Return(f1.User)
	fsFile.On("User").Return(f1.User)
	f.On("Group").Return(f1.Group)
	fsFile.On("Group").Return(f2.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f1.Path)
	f.On("Manage").Return(nil)
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	expected := []changelog.Event{{
		Event:   "file.managed",
		Message: "group: vaspahomov -> root\n",
	}}
	assert.Equal(t, expected, chl.Events)
}

func TestManage_Execute_AllChanged(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f2.Content)
	fsFile.On("Content").Return(f1.Content)
	f.On("Mode").Return(f2.Mode)
	fsFile.On("Mode").Return(f1.Mode)
	f.On("User").Return(f2.User)
	fsFile.On("User").Return(f1.User)
	f.On("Group").Return(f2.Group)
	fsFile.On("Group").Return(f2.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f2.Path)
	f.On("Manage").Return(nil)
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	if err != nil {
		t.Fatal(err)
	}

	expected := []changelog.Event{{
		Event:   "file.managed",
		Message: "content: |\n  --- before\n  +++ after\n  @@ -1 +1 @@\n  -test\n  +test2\nmode: 644 -> 777\nuser: root -> vaspahomov\n",
	}}
	assert.Equal(t, expected, chl.Events)
}

func TestManage_Execute_ManageFail(t *testing.T) {
	f := file.NewFileMock()
	fsFile := file.NewFileMock()
	f.On("FromFs").Return(fsFile, nil)
	f.On("Content").Return(f2.Content)
	fsFile.On("Content").Return(f1.Content)
	f.On("Mode").Return(f2.Mode)
	fsFile.On("Mode").Return(f1.Mode)
	f.On("User").Return(f2.User)
	fsFile.On("User").Return(f1.User)
	f.On("Group").Return(f2.Group)
	fsFile.On("Group").Return(f2.Group)
	f.On("Path").Return(f1.Path)
	fsFile.On("Path").Return(f2.Path)
	f.On("Manage").Return(errors.New("failed to manage"))
	fsFile.On("Manage").Return(nil)
	files := []*pb.ManagedFile{f1}
	chl := changelog.New()

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	err := manage.Execute(envFromFile(f), chl)
	assert.Error(t, err)
	assert.Equal(t, changelog.New(), chl)
}

func TestManage_Plan_OneFile(t *testing.T) {
	files := []*pb.ManagedFile{f1}
	p := make([]map[string]string, 0)

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	p = manage.Plan(p)
	assert.Equal(t, fmt.Sprintf("paths: {%s}", f1.Path), p[0]["file.manage"])
}

func TestManage_Plan_TwoFile(t *testing.T) {
	files := []*pb.ManagedFile{f1, f2}
	p := make([]map[string]string, 0)

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	p = manage.Plan(p)
	assert.Equal(t, fmt.Sprintf("paths: {%s}", strings.Join([]string{f1.Path, f2.Path}, ", ")), p[0]["file.manage"])
}

func TestManage_Plan_Empty(t *testing.T) {
	files := make([]*pb.ManagedFile, 0)
	p := make([]map[string]string, 0)

	manage := tasks.NewManage(files, tasks.NewSimpleCond(&pb.Condition{}))
	p = manage.Plan(p)
	assert.Equal(t, 0, len(p))
}
