package tests

import (
	"errors"
	"fmt"
	"strings"
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/changelog"
	"a.yandex-team.ru/infra/hostctl/internal/slot"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman/dpkgutil"
	"a.yandex-team.ru/infra/hostctl/internal/units/revjob"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

var l, _ = zap.New(zap.CLIConfig(log.DebugLevel))

var f1 = &pb.ManagedFile{
	Path:    "/tmp/f1",
	Content: "f1",
	User:    "vaspahomov",
	Group:   "vaspahomov",
	Mode:    "0644",
}

var f2 = &pb.ManagedFile{
	Path:    "/tmp/f2",
	Content: "f2",
	User:    "vaspahomov",
	Group:   "vaspahomov",
	Mode:    "0644",
}

var p1 = &pb.SystemPackage{
	Name:    "p1",
	Version: "ver-1",
}

var p1v2 = &pb.SystemPackage{
	Name:    "p1",
	Version: "ver-2",
}

var p2 = &pb.SystemPackage{
	Name:    "p2",
	Version: "ver-1",
}

var p3 = &pb.SystemPackage{
	Name:    "p3",
	Version: "ver-1",
}

func TestPdToJobs_JobsPlan_InstallPkg(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{{"package.install": fmt.Sprintf("pkgs: {%s}", p1.Name+"="+p1.Version)}},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_DoNotUninstallPkg_ForgetSpecified(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p2},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta: &pb.SlotMeta{
			SkipRemovePhase: &pb.SkipRemovePhase{Packages: []string{p1.Name, p2.Name}},
		},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		revjob.SkipRemovingTasks(j, s.Meta.SkipRemovePhase)
		plans = append(plans, revjob.Plan(j))
	}
	assert.Equal(t, []tasks.Plan{
		{},
		{{"package.install": fmt.Sprintf("pkgs: {%s}", p1.Name+"="+p1.Version)}},
	}, plans)
}

func TestPdToJobs_JobsPlan_DoNotRemoveActualPkg(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta: &pb.SlotMeta{},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		{},
		[]map[string]string{{"package.install": fmt.Sprintf("pkgs: {%s}", p1.Name+"="+p1.Version)}},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_RemovePkg(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{{"package.removed": fmt.Sprintf("pkgs: {%s}", p1.Name)}},
		{},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_UpdatePkg(t *testing.T) {
	// not implemented yet
	t.Skip()

	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1v2},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{{"package.update": fmt.Sprintf("pkgs: {%s}", p1v2.Name+"="+p1v2.Version)}},
	}

	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_ManageFile(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta: &pb.SlotMeta{},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		{},
		[]map[string]string{{"file.manage": fmt.Sprintf("paths: {%s}", f1.Path)}},
	}

	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_DoNotManageFile(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}

	expected := []tasks.Plan{
		{},
		[]map[string]string{{"file.manage": fmt.Sprintf("paths: {%s}", f1.Path)}},
	}

	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsPlan_RemoveFile(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{{"file.removed": fmt.Sprintf("paths: {%s}", f1.Path)}},
		{},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_ComplexJobsPlan(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1, p2},
				Files:    []*pb.ManagedFile{f1, f2},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1, p3},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{{"package.removed": fmt.Sprintf("pkgs: {%s}", p3.Name)}},
		[]map[string]string{{"package.install": fmt.Sprintf("pkgs: {%s}", strings.Join([]string{p1.Name + "=" + p1.Version, p2.Name + "=" + p2.Version}, ", "))}, {"file.manage": fmt.Sprintf("paths: {%s}", strings.Join([]string{f1.Path, f2.Path}, ", "))}},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsExecute_InstallPkg(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}
	m := pacman.NewDPKGMock()
	m.On("List", []string{p1.Name}).Return(map[string]*dpkgutil.PackageStatus{p1.Name: {
		Name:      p1.Name,
		Version:   p1.Version,
		Installed: false,
	}}, nil).Once()
	m.On("InstallSet", mock.Anything).Return(nil)
	e := &env.Env{
		Pacman: m,
		L:      l,
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	pl := changelog.New()
	for _, j := range jobs {
		var err error
		pl, err = j.Execute(e)
		if err != nil {
			t.Fatal(err)
		}
	}

	expected := changelog.New()
	expected.Add("pkg.installed", p1.Name+"="+p1.Version)
	assert.Equal(t, expected, pl)
}

func TestPdToJobs_JobsExecute_ShouldNotInstallAlreadyInstalledPkg(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
				Packages: []*pb.SystemPackage{p1},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}
	m := pacman.NewDPKGMock()
	m.On("List", []string{p1.Name}).Return(map[string]*dpkgutil.PackageStatus{p1.Name: {
		Name:      p1.Name,
		Version:   p1.Version,
		Installed: true,
	}}, nil).Once()
	m.On("InstallSet", mock.Anything).Return(nil)
	e := &env.Env{
		Pacman: m,
		L:      l,
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	pl := changelog.New()
	for _, j := range jobs {
		var err error
		pl, err = j.Execute(e)
		if err != nil {
			t.Fatal(err)
		}
	}
	expected := changelog.New()
	assert.Equal(t, expected, pl)
}

func TestPdToJobs_JobsExecute_FailedToInstall(t *testing.T) {
	r := &pb.Rev{
		Id:     "",
		Target: pb.RevisionTarget_CURRENT,
		Spec: &pb.Rev_PackageSet{PackageSet: &pb.PackageSetSpec{
			Packages: []*pb.SystemPackage{p1},
			Files:    []*pb.ManagedFile{},
		}},
		Meta: &pb.RevisionMeta{},
	}
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{r},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
		Meta: &pb.SlotMeta{},
	}
	m := pacman.NewDPKGMock()
	m.On("List", []string{p1.Name}).Return(map[string]*dpkgutil.PackageStatus{p1.Name: {
		Name:      p1.Name,
		Version:   p1.Version,
		Installed: false,
	}}, nil)
	m.On("GetPackageStatus", p1.Name).Return(&dpkgutil.PackageStatus{
		Name:      p1.Name,
		Version:   p1.Version,
		Installed: false,
	}, nil)
	m.On("InstallSet", mock.Anything).Return(errors.New("failed to install"))
	e := &env.Env{
		Pacman: m,
		L:      l,
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	pl := changelog.New()
	for _, j := range jobs {
		var err error
		pl, err = j.Execute(e)
		assert.Error(t, err)
	}
	expected := changelog.New()
	assert.Equal(t, expected, pl)
	assert.Equal(t, "False", s.Status.Installed.Status)
	assert.Equal(t, "failed to install", s.Status.Installed.Message)
}

func TestSDServiceToJobs_JobsPlan_SD_Actions_Current(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start": "mock.service"},
			{"systemd.enable": "mock.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Template_Actions_Current(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Actions_Current(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start[oneshot]": "mock.service"},
			{"systemd.enable": "mock.service"},
			{"systemd.daemon-reload": "mock.timer"},
			{"systemd.start[timer]": "mock.timer"},
			{"systemd.enable": "mock.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Template_Actions_Current(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	jobs := revjob.FromSlot(slot.NewSlot(s))
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start[oneshot]": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock1.timer"},
			{"systemd.start[timer]": "mock@mock1.timer"},
			{"systemd.enable": "mock@mock1.timer"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start[oneshot]": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock2.timer"},
			{"systemd.start[timer]": "mock@mock2.timer"},
			{"systemd.enable": "mock@mock2.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Actions_Reinstall(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewReinstallFromRevAndSlot("mock", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start": "mock.service"},
			{"systemd.enable": "mock.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Template_Actions_Reinstall(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewReinstallFromRevAndSlot("mock@", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Actions_Reinstall(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewReinstallFromRevAndSlot("mock", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.timer"},
			{"systemd.stop": "mock.timer"},
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start[oneshot]": "mock.service"},
			{"systemd.enable": "mock.service"},
			{"systemd.daemon-reload": "mock.timer"},
			{"systemd.start[timer]": "mock.timer"},
			{"systemd.enable": "mock.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Template_Actions_Reinstall(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewReinstallFromRevAndSlot("mock@", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.timer"},
			{"systemd.stop": "mock@mock1.timer"},
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.timer"},
			{"systemd.stop": "mock@mock2.timer"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start[oneshot]": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock1.timer"},
			{"systemd.start[timer]": "mock@mock1.timer"},
			{"systemd.enable": "mock@mock1.timer"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start[oneshot]": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock2.timer"},
			{"systemd.start[timer]": "mock@mock2.timer"},
			{"systemd.enable": "mock@mock2.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Actions_Restart(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewRestartFromSlotAndRev("mock", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start": "mock.service"},
			{"systemd.enable": "mock.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Template_Actions_Restart(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewRestartFromSlotAndRev("mock@", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Actions_Restart(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec:   &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewRestartFromSlotAndRev("mock", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.timer"},
			{"systemd.stop": "mock.timer"},
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
			{"systemd.daemon-reload": "mock.service"},
			{"systemd.start[oneshot]": "mock.service"},
			{"systemd.enable": "mock.service"},
			{"systemd.daemon-reload": "mock.timer"},
			{"systemd.start[timer]": "mock.timer"},
			{"systemd.enable": "mock.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Template_Actions_Restart(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.NewRestartFromSlotAndRev("mock@", slot.NewRevision(s.Revs[0]), slot.NewSlot(s))
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.timer"},
			{"systemd.stop": "mock@mock1.timer"},
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.timer"},
			{"systemd.stop": "mock@mock2.timer"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock1.service"},
			{"systemd.start[oneshot]": "mock@mock1.service"},
			{"systemd.enable": "mock@mock1.service"},
			{"systemd.daemon-reload": "mock@mock1.timer"},
			{"systemd.start[timer]": "mock@mock1.timer"},
			{"systemd.enable": "mock@mock1.timer"},
			{"systemd.daemon-reload": "mock@mock2.service"},
			{"systemd.start[oneshot]": "mock@mock2.service"},
			{"systemd.enable": "mock@mock2.service"},
			{"systemd.daemon-reload": "mock@mock2.timer"},
			{"systemd.start[timer]": "mock@mock2.timer"},
			{"systemd.enable": "mock@mock2.timer"},
		},
	}
	assert.Equal(t, expected, plans)
}

///////////////////////////////////////////////////////////////////////////////////////////////
func TestSDServiceToJobs_JobsPlan_SD_Actions_Removed(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec:   &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.FromRev(slot.NewRevision(s.Revs[0]), slot.NewSlot(s).Status(), "mock")
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDServiceToJobs_JobsPlan_SD_Template_Actions_Removed(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_SystemService{SystemService: &pb.SystemServiceSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.FromRev(slot.NewRevision(s.Revs[0]), slot.NewSlot(s).Status(), "mock@")
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Actions_Removed(t *testing.T) {
	s := &pb.Slot{
		Name: "mock",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec:   &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{}},
			Meta:   &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.FromRev(slot.NewRevision(s.Revs[0]), slot.NewSlot(s).Status(), "mock")
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock.timer"},
			{"systemd.stop": "mock.timer"},
			{"systemd.disable": "mock.service"},
			{"systemd.stop": "mock.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestSDTimerToJobs_JobsPlan_SD_Template_Actions_Removed(t *testing.T) {
	s := &pb.Slot{
		Name: "mock@",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_TimerJob{TimerJob: &pb.TimerJobSpec{
				Template: &pb.SystemdTemplate{Instances: []string{"mock1", "mock2"}},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta:   &pb.SlotMeta{},
		Status: &pb.SlotStatus{},
	}
	job := revjob.FromRev(slot.NewRevision(s.Revs[0]), slot.NewSlot(s).Status(), "mock@")
	plans := []tasks.Plan{revjob.Plan(job)}
	expected := []tasks.Plan{
		[]map[string]string{
			{"systemd.disable": "mock@mock1.timer"},
			{"systemd.stop": "mock@mock1.timer"},
			{"systemd.disable": "mock@mock1.service"},
			{"systemd.stop": "mock@mock1.service"},
			{"systemd.disable": "mock@mock2.timer"},
			{"systemd.stop": "mock@mock2.timer"},
			{"systemd.disable": "mock@mock2.service"},
			{"systemd.stop": "mock@mock2.service"},
		},
	}
	assert.Equal(t, expected, plans)
}

func TestPdToJobs_JobsExecute_Managefile(t *testing.T) {
	s := &pb.Slot{
		Name: "test_slot",
		Revs: []*pb.Rev{{
			Id:     "",
			Target: pb.RevisionTarget_CURRENT,
			Spec: &pb.Rev_PortoDaemon{PortoDaemon: &pb.PortoDaemon{
				Properties: &pb.PortoProperties{
					Cmd: []string{"/bin/sleep 10"},
				},
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{f1},
			}},
			Meta: &pb.RevisionMeta{},
		}, {
			Id:     "",
			Target: pb.RevisionTarget_REMOVED,
			Spec: &pb.Rev_PortoDaemon{PortoDaemon: &pb.PortoDaemon{
				Properties: &pb.PortoProperties{
					Cmd: []string{"/bin/sleep 10"},
				},
				Packages: []*pb.SystemPackage{},
				Files:    []*pb.ManagedFile{},
			}},
			Meta: &pb.RevisionMeta{},
		}},
		Meta: &pb.SlotMeta{},
		Status: &pb.SlotStatus{
			Installed: &pb.Condition{},
		},
	}

	jobs := revjob.FromSlot(slot.NewSlot(s))
	jobs.Prune()
	plans := make([]tasks.Plan, 0)
	for _, j := range jobs {
		plans = append(plans, revjob.Plan(j))
	}
	expected := []tasks.Plan{
		{},
		[]map[string]string{
			{"file.manage": fmt.Sprintf("paths: {%s}", f1.Path)},
			{"porto.container": "name: test_slot\n properties:\n capabilities: []\ncapabilities_ambient: []\ncmd:\n- /bin/sleep 10\ncontrollers: {}\ncpu_guarantee: \"\"\ncpu_limit: \"\"\ngroup: \"\"\nisolate: \"\"\nmax_respawns: \"0\"\nmemory_guarantee: \"\"\nmemory_limit: \"\"\nrespawn_delay: \"0\"\nulimit: {}\nuser: \"\"\nvirt_mode: \"\"\n"}},
	}

	assert.Equal(t, expected, plans)
}

func TestPodCurrentPlan(t *testing.T) {
	slotStatus := slot.NewEmptySlotStatus()
	rev := slot.NewRevision(&pb.Rev{
		Id: "test-rev",
		Meta: &pb.RevisionMeta{
			Kind:    "HostPod",
			Version: "test-ver",
		},
		Target: pb.RevisionTarget_CURRENT,
		Spec: &pb.Rev_Pod{
			Pod: &pb.HostPodSpec{
				Packages: []*pb.SystemPackage{
					{
						Name:    "test-package",
						Version: "test-ver",
					},
				},
				Files: []*pb.ManagedFile{
					{
						Path:    "/test-file",
						Content: "test-content",
						User:    "mock",
						Group:   "mock",
						Mode:    "666",
					},
				},
				PortoDaemons: []*pb.HostPodPortoDaemon{
					{
						Name: "test-pd",
						Properties: &pb.PortoProperties{
							Cmd: []string{"/test-cmd"},
						},
					},
				},
				Services: []*pb.HostPodService{
					{
						Name:         "test-service",
						UpdatePolicy: nil,
						Template:     nil,
					},
				},
				Timers: []*pb.HostPodTimer{
					{
						Name: "test-timer",
					},
				},
			},
		},
	})
	podPlan := revjob.Plan(revjob.CurrentFromRevision("test-pod", rev, slotStatus))
	expectedPlan := tasks.Plan{
		{"package.install": "pkgs: {test-package=test-ver}"},
		{"file.manage": "paths: {/test-file}"},
		{"systemd.daemon-reload": "test-service.service"},
		{"systemd.start": "test-service.service"},
		{"systemd.enable": "test-service.service"},
		{"systemd.daemon-reload": "test-timer.service"},
		{"systemd.start[oneshot]": "test-timer.service"},
		{"systemd.enable": "test-timer.service"},
		{"systemd.daemon-reload": "test-timer.timer"},
		{"systemd.start[timer]": "test-timer.timer"},
		{"systemd.enable": "test-timer.timer"},
		{"porto.container": "name: test-pd\n properties:\n capabilities: []\ncapabilities_ambient: []\ncmd:\n- /test-cmd\ncontrollers: {}\ncpu_guarantee: \"\"\ncpu_limit: \"\"\ngroup: \"\"\nisolate: \"\"\nmax_respawns: \"0\"\nmemory_guarantee: \"\"\nmemory_limit: \"\"\nrespawn_delay: \"0\"\nulimit: {}\nuser: \"\"\nvirt_mode: \"\"\n"},
	}
	assert.Equal(t, expectedPlan, podPlan)
}

func TestPodRemovedPlan(t *testing.T) {
	slotStatus := slot.NewEmptySlotStatus()
	rev := slot.NewRevision(&pb.Rev{
		Id: "test-rev",
		Meta: &pb.RevisionMeta{
			Kind:    "HostPod",
			Version: "test-ver",
		},
		Target: pb.RevisionTarget_REMOVED,
		Spec: &pb.Rev_Pod{
			Pod: &pb.HostPodSpec{
				Packages: []*pb.SystemPackage{
					{
						Name:    "test-package",
						Version: "test-ver",
					},
				},
				Files: []*pb.ManagedFile{
					{
						Path:    "/test-file",
						Content: "test-content",
						User:    "mock",
						Group:   "mock",
						Mode:    "666",
					},
				},
				PortoDaemons: []*pb.HostPodPortoDaemon{
					{
						Name: "test-pd",
						Properties: &pb.PortoProperties{
							Cmd: []string{"/test-cmd"},
						},
					},
				},
				Services: []*pb.HostPodService{
					{
						Name:         "test-service",
						UpdatePolicy: nil,
						Template:     nil,
					},
				},
				Timers: []*pb.HostPodTimer{
					{
						Name: "test-timer",
					},
				},
			},
		},
	})
	podPlan := revjob.Plan(revjob.RemovedFromRevision("test-pod", rev, slotStatus))
	expectedPlan := tasks.Plan{
		{"porto.destroyed": "test-pd"},
		{"systemd.disable": "test-timer.timer"},
		{"systemd.stop": "test-timer.timer"},
		{"systemd.disable": "test-timer.service"},
		{"systemd.stop": "test-timer.service"},
		{"systemd.disable": "test-service.service"},
		{"systemd.stop": "test-service.service"},
		{"file.removed": "paths: {/test-file}"},
		{"package.removed": "pkgs: {test-package}"},
	}
	assert.Equal(t, expectedPlan, podPlan)
}

func TestPodRestartPlan(t *testing.T) {
	s := slot.New("test-pod")
	rev := slot.NewRevision(&pb.Rev{
		Id: "test-rev",
		Meta: &pb.RevisionMeta{
			Kind:    "HostPod",
			Version: "test-ver",
		},
		Target: pb.RevisionTarget_CURRENT,
		Spec: &pb.Rev_Pod{
			Pod: &pb.HostPodSpec{
				Packages: []*pb.SystemPackage{
					{
						Name:    "test-package",
						Version: "test-ver",
					},
				},
				Files: []*pb.ManagedFile{
					{
						Path:    "/test-file",
						Content: "test-content",
						User:    "mock",
						Group:   "mock",
						Mode:    "666",
					},
				},
				PortoDaemons: []*pb.HostPodPortoDaemon{
					{
						Name: "test-pd",
						Properties: &pb.PortoProperties{
							Cmd: []string{"/test-cmd"},
						},
					},
				},
				Services: []*pb.HostPodService{
					{
						Name:         "test-service",
						UpdatePolicy: nil,
						Template:     nil,
					},
				},
				Timers: []*pb.HostPodTimer{
					{
						Name: "test-timer",
					},
				},
			},
		},
	})
	s.SetRevs([]*slot.Rev{rev})
	podPlan := revjob.Plan(revjob.NewRestartFromSlotAndRev("test-pod", rev, s))
	expectedPlan := tasks.Plan{
		{"systemd.disable": "test-service.service"},
		{"systemd.stop": "test-service.service"},
		{"systemd.daemon-reload": "test-service.service"},
		{"systemd.start": "test-service.service"},
		{"systemd.enable": "test-service.service"},

		{"systemd.disable": "test-timer.timer"},
		{"systemd.stop": "test-timer.timer"},
		{"systemd.disable": "test-timer.service"},
		{"systemd.stop": "test-timer.service"},
		{"systemd.daemon-reload": "test-timer.service"},
		{"systemd.start[oneshot]": "test-timer.service"},
		{"systemd.enable": "test-timer.service"},
		{"systemd.daemon-reload": "test-timer.timer"},
		{"systemd.start[timer]": "test-timer.timer"},
		{"systemd.enable": "test-timer.timer"},

		{"porto.destroyed": "test-pod"},
		{"porto.container": "name: test-pd\n properties:\n capabilities: []\ncapabilities_ambient: []\ncmd:\n- /test-cmd\ncontrollers: {}\ncpu_guarantee: \"\"\ncpu_limit: \"\"\ngroup: \"\"\nisolate: \"\"\nmax_respawns: \"0\"\nmemory_guarantee: \"\"\nmemory_limit: \"\"\nrespawn_delay: \"0\"\nulimit: {}\nuser: \"\"\nvirt_mode: \"\"\n"},
	}
	assert.Equal(t, expectedPlan, podPlan)
}

func TestPodReinstallPlan(t *testing.T) {
	s := slot.New("test-pod")
	rev := slot.NewRevision(&pb.Rev{
		Id: "test-rev",
		Meta: &pb.RevisionMeta{
			Kind:    "HostPod",
			Version: "test-ver",
		},
		Target: pb.RevisionTarget_CURRENT,
		Spec: &pb.Rev_Pod{
			Pod: &pb.HostPodSpec{
				Packages: []*pb.SystemPackage{
					{
						Name:    "test-package",
						Version: "test-ver",
					},
				},
				Files: []*pb.ManagedFile{
					{
						Path:    "/test-file",
						Content: "test-content",
						User:    "mock",
						Group:   "mock",
						Mode:    "666",
					},
				},
				PortoDaemons: []*pb.HostPodPortoDaemon{
					{
						Name: "test-pd",
						Properties: &pb.PortoProperties{
							Cmd: []string{"/test-cmd"},
						},
					},
				},
				Services: []*pb.HostPodService{
					{
						Name:         "test-service",
						UpdatePolicy: nil,
						Template:     nil,
					},
				},
				Timers: []*pb.HostPodTimer{
					{
						Name: "test-timer",
					},
				},
			},
		},
	})
	s.SetRevs([]*slot.Rev{rev})
	podPlan := revjob.Plan(revjob.NewReinstallFromRevAndSlot("test-pod", rev, s))
	expectedPlan := tasks.Plan{
		{"porto.destroyed": "test-pd"},
		{"systemd.disable": "test-timer.timer"},
		{"systemd.stop": "test-timer.timer"},
		{"systemd.disable": "test-timer.service"},
		{"systemd.stop": "test-timer.service"},
		{"systemd.disable": "test-service.service"},
		{"systemd.stop": "test-service.service"},
		{"file.removed": "paths: {/test-file}"},
		{"package.removed": "pkgs: {test-package}"},

		{"package.install": "pkgs: {test-package=test-ver}"},
		{"file.manage": "paths: {/test-file}"},
		{"systemd.daemon-reload": "test-service.service"},
		{"systemd.start": "test-service.service"},
		{"systemd.enable": "test-service.service"},
		{"systemd.daemon-reload": "test-timer.service"},
		{"systemd.start[oneshot]": "test-timer.service"},
		{"systemd.enable": "test-timer.service"},
		{"systemd.daemon-reload": "test-timer.timer"},
		{"systemd.start[timer]": "test-timer.timer"},
		{"systemd.enable": "test-timer.timer"},
		{"porto.container": "name: test-pd\n properties:\n capabilities: []\ncapabilities_ambient: []\ncmd:\n- /test-cmd\ncontrollers: {}\ncpu_guarantee: \"\"\ncpu_limit: \"\"\ngroup: \"\"\nisolate: \"\"\nmax_respawns: \"0\"\nmemory_guarantee: \"\"\nmemory_limit: \"\"\nrespawn_delay: \"0\"\nulimit: {}\nuser: \"\"\nvirt_mode: \"\"\n"},
	}
	assert.Equal(t, expectedPlan, podPlan)
}
