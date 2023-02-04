package tests

import (
	"fmt"
	"strings"
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman/dpkgutil"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/test/assertpb"
)

var l, _ = zap.New(zap.CLIConfig(log.DebugLevel))

func TestInstall_Packages(t *testing.T) {
	pkgs := []*pb.SystemPackage{{
		Name:    "p1",
		Version: "ver-1",
	}, {
		Name:    "p1",
		Version: "ver-2",
	}, {
		Name:    "p2",
		Version: "ver-1",
	}}
	install := tasks.NewInstall(pkgs, tasks.NewSimpleCond(&pb.Condition{}))
	assertpb.Equal(t, pkgs, install.Packages())
}

func TestInstall_Plan(t *testing.T) {
	pkgs := []*pb.SystemPackage{{
		Name:    "p1",
		Version: "ver-1",
	}, {
		Name:    "p1",
		Version: "ver-2",
	}, {
		Name:    "p2",
		Version: "ver-1",
	}}
	install := tasks.NewInstall(pkgs, tasks.NewSimpleCond(&pb.Condition{}))
	plan := make([]map[string]string, 0)
	plan = install.Plan(plan)
	assert.Equal(t, plan[0]["package.install"], fmt.Sprintf("pkgs: {%s}", strings.Join([]string{"p1=ver-1", "p1=ver-2", "p2=ver-1"}, ", ")))
}

// when all packages installed should install all
func TestInstall_Execute_AllPackagesUninstalled(t *testing.T) {
	m := pacman.NewDPKGMock()
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	pkgs := []*pb.SystemPackage{{
		Name:    "p1",
		Version: "ver-1",
	}, {
		Name:    "p2",
		Version: "ver-1",
	}}
	pkgList := make(map[string]*dpkgutil.PackageStatus, len(pkgs))
	pkgNames := make([]string, len(pkgs))
	for i, p := range pkgs {
		pkgList[p.Name] = &dpkgutil.PackageStatus{
			Installed: false,
			Name:      p.Name,
			Version:   p.Version,
		}
		pkgNames[i] = p.Name
	}
	m.On("List", pkgNames).Return(pkgList, nil)
	install := tasks.NewInstall(pkgs, tasks.NewSimpleCond(&pb.Condition{}))
	m.On("InstallSet", mock.Anything).Return(nil)
	pl := changelog.New()
	err := install.Execute(e, pl)
	if err != nil {
		t.Fatal(err)
	}
	expected := changelog.New()
	for _, p := range pkgs {
		expected.Events = append(expected.Events, changelog.Event{
			Event:   "pkg.installed",
			Message: p.Name + "=" + p.Version,
		})
	}
	assert.Equal(t, expected, pl)
}

// when all packages installed should not do anything
func TestInstall_Execute_AllPackagesInstalled(t *testing.T) {
	m := pacman.NewDPKGMock()
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	pkgs := []*pb.SystemPackage{{
		Name:    "p1",
		Version: "ver-1",
	}, {
		Name:    "p2",
		Version: "ver-1",
	}}
	pkgList := make(map[string]*dpkgutil.PackageStatus, len(pkgs))
	pkgNames := make([]string, len(pkgs))
	for i, p := range pkgs {
		pkgList[p.Name] = &dpkgutil.PackageStatus{
			Installed: true,
			Name:      p.Name,
			Version:   p.Version,
		}
		pkgNames[i] = p.Name
	}
	m.On("List", pkgNames).Return(pkgList, nil)
	install := tasks.NewInstall(pkgs, tasks.NewSimpleCond(&pb.Condition{}))
	m.On("InstallSet", mock.Anything).Return(nil)

	pl := changelog.New()
	err := install.Execute(e, pl)
	if err != nil {
		t.Fatal(err)
	}
	expected := changelog.New()
	assert.Equal(t, expected, pl)
}

// when installed old package versions should update
func TestInstall_Execute_OldPackagesVersions(t *testing.T) {
	// need rework
	t.Skip()

	m := pacman.NewDPKGMock()
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	pkgs := []*pb.SystemPackage{{
		Name:    "p1",
		Version: "ver-2",
	}, {
		Name:    "p2",
		Version: "ver-2",
	}}
	pkgList := make(map[string]*dpkgutil.PackageStatus, len(pkgs))
	pkgNames := make([]string, len(pkgs))
	for i, p := range pkgs {
		pkgList[p.Name] = &dpkgutil.PackageStatus{
			Installed: false,
			Name:      p.Name,
			Version:   "ver-1",
		}
		pkgNames[i] = p.Name
	}
	m.On("List", pkgNames).Return(pkgList, nil).Once()
	install := tasks.NewInstall(pkgs, tasks.NewSimpleCond(&pb.Condition{}))
	m.On("InstallSet", mock.Anything).Return(nil)

	pl := changelog.New()
	err := install.Execute(e, pl)
	if err != nil {
		t.Fatal(err)
	}
	expected := make([]changelog.Event, 0)
	for _, p := range pkgs {
		expected = append(expected, changelog.Event{
			Event:   "pkg.installed",
			Message: p.Name + "=" + p.Version,
		})
	}
	assert.Equal(t, expected, pl)
}
