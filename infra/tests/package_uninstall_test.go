package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman/dpkgutil"
	pb "a.yandex-team.ru/infra/hostctl/proto"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
)

func TestUninstall_Packages(t *testing.T) {
	names := []string{"p1", "p2"}
	uninstall := tasks.NewUninstall(names, tasks.NewSimpleCond(&pb.Condition{}))

	assert.Equal(t, names, uninstall.Names())
}

func TestUninstall_Plan(t *testing.T) {
	names := []string{"p1", "p2"}
	uninstall := tasks.NewUninstall(names, tasks.NewSimpleCond(&pb.Condition{}))

	plan := make([]map[string]string, 0)
	plan = uninstall.Plan(plan)

	assert.Equal(t, plan[0]["package.removed"], "pkgs: {p1, p2}")
}

func TestUninstall_Prune(t *testing.T) {
	names := []string{"p1", "p2"}
	uninstall := tasks.NewUninstall(names, tasks.NewSimpleCond(&pb.Condition{}))

	uninstall.Prune([]string{"p1"})
	pruned := uninstall.Names()

	assert.Equal(t, pruned, []string{"p2"})
}

func TestUninstall_Execute_AllPackagesInstalled(t *testing.T) {
	m := pacman.NewDPKGMock()
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	names := []string{"p1", "p2"}
	m.On("List", names).Return(map[string]*dpkgutil.PackageStatus{"p1": {
		Name:      "p1",
		Version:   "ver-1",
		Installed: true,
	}, "p2": {
		Name:      "p2",
		Version:   "ver-1",
		Installed: true,
	}}, nil)
	task := tasks.NewUninstall(names, tasks.NewSimpleCond(&pb.Condition{}))
	m.On("PurgeSet", mock.Anything).Return(nil)
	pl := changelog.New()
	err := task.Execute(e, pl)
	if err != nil {
		t.Fatal(err)
	}
	m.AssertNumberOfCalls(t, "PurgeSet", 1)
	logsOk := true
	assert.Len(t, pl.Events, 2)
	for _, n := range names {
		ok := false
		for _, e := range pl.Events {
			if e.Event == "pkg.purged" && e.Message == n {
				ok = true
			}
		}
		if !ok {
			logsOk = false
		}
	}
	if !logsOk {
		fmtCh, _ := pl.Fmt("", "", "")
		t.Errorf("invalid changelog %s for [p1, p2] pkgs", fmtCh)
	}
}

func TestUninstall_Execute_AllPackagesUninstalled(t *testing.T) {
	m := pacman.NewDPKGMock()
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	names := []string{"p1", "p2"}
	m.On("List", names).Return(map[string]*dpkgutil.PackageStatus{"p1": {
		Name:      "p1",
		Version:   "ver-1",
		Installed: false,
	}, "p2": {
		Name:      "p2",
		Version:   "ver-1",
		Installed: false,
	}}, nil)
	task := tasks.NewUninstall(names, tasks.NewSimpleCond(&pb.Condition{}))
	m.On("PurgeSet", mock.Anything).Return(nil)

	pl := changelog.New()
	err := task.Execute(e, pl)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, changelog.New(), pl)
}
