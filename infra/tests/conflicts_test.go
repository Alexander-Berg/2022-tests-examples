package tests

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/infra/hostctl/internal/units/env"
	"a.yandex-team.ru/infra/hostctl/internal/units/env/pacman"
	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
)

var (
	apt1 = &pb.SystemPackage{
		Name:    "apt",
		Version: "1.2.32ubuntu0.1",
	}
	apt2 = &pb.SystemPackage{
		Name:    "apt",
		Version: "1.2.32",
	}
	apt3 = &pb.SystemPackage{
		Name:    "apt",
		Version: "1.2.33",
	}
	git = &pb.SystemPackage{
		Name:    "git",
		Version: "1:2.7.4-0ubuntu1.9",
	}
)

func TestCheckConflicts(t *testing.T) {
	type args struct {
		e         *env.Env
		install   []*tasks.PackageInstall
		uninstall []*tasks.PackageUninstall
		remove    []*tasks.FileRemove
		manage    []*tasks.FileManage
	}
	p := pacman.NewDPKGMock()
	p.On("InstallSetDryRun", mock.Anything).Return(nil)
	e := &env.Env{
		Pacman: p,
	}
	tests := []struct {
		name string
		args args
		want string
	}{{
		name: "no conflicts packages",
		args: args{
			e:         e,
			install:   []*tasks.PackageInstall{tasks.NewInstall([]*pb.SystemPackage{git}, tasks.NewSimpleCond(&pb.Condition{})), tasks.NewInstall([]*pb.SystemPackage{apt1}, tasks.NewSimpleCond(&pb.Condition{}))},
			uninstall: make([]*tasks.PackageUninstall, 0),
			remove:    make([]*tasks.FileRemove, 0),
			manage:    make([]*tasks.FileManage, 0),
		},
		want: "",
	}, {
		name: "conflicted packages",
		args: args{
			e:         e,
			install:   []*tasks.PackageInstall{tasks.NewInstall([]*pb.SystemPackage{apt1}, tasks.NewSimpleCond(&pb.Condition{})), tasks.NewInstall([]*pb.SystemPackage{apt2}, tasks.NewSimpleCond(&pb.Condition{}))},
			uninstall: make([]*tasks.PackageUninstall, 0),
			remove:    make([]*tasks.FileRemove, 0),
			manage:    make([]*tasks.FileManage, 0),
		},
		want: "different version in same pkg 'apt': '1.2.32ubuntu0.1', '1.2.32'",
	}, {
		name: "multiple conflicted packages",
		args: args{
			e:         e,
			install:   []*tasks.PackageInstall{tasks.NewInstall([]*pb.SystemPackage{apt1}, tasks.NewSimpleCond(&pb.Condition{})), tasks.NewInstall([]*pb.SystemPackage{apt2}, tasks.NewSimpleCond(&pb.Condition{})), tasks.NewInstall([]*pb.SystemPackage{apt3}, tasks.NewSimpleCond(&pb.Condition{}))},
			uninstall: make([]*tasks.PackageUninstall, 0),
			remove:    make([]*tasks.FileRemove, 0),
			manage:    make([]*tasks.FileManage, 0),
		},
		want: "different version in same pkg 'apt': '1.2.32ubuntu0.1', '1.2.32';different version in same pkg 'apt': '1.2.32ubuntu0.1', '1.2.33';different version in same pkg 'apt': '1.2.32', '1.2.33'",
	}, {
		name: "conflicted files",
		args: args{
			e:         e,
			install:   make([]*tasks.PackageInstall, 0),
			uninstall: make([]*tasks.PackageUninstall, 0),
			remove:    make([]*tasks.FileRemove, 0),
			manage: []*tasks.FileManage{
				tasks.NewManage([]*pb.ManagedFile{{
					Path:    "/tmp/a",
					Content: "a",
					User:    "root",
					Group:   "root",
					Mode:    "0664",
				}}, tasks.NewSimpleCond(&pb.Condition{})),
				tasks.NewManage([]*pb.ManagedFile{{
					Path:    "/tmp/a",
					Content: "b",
					User:    "root",
					Group:   "root",
					Mode:    "0664",
				}}, tasks.NewSimpleCond(&pb.Condition{}))},
		},
		want: "different contents in same file '/tmp/a': 'a', 'b'",
	}, {
		name: "no conflicts files",
		args: args{
			e:         e,
			install:   make([]*tasks.PackageInstall, 0),
			uninstall: make([]*tasks.PackageUninstall, 0),
			remove:    make([]*tasks.FileRemove, 0),
			manage: []*tasks.FileManage{
				tasks.NewManage([]*pb.ManagedFile{{
					Path:    "/tmp/a",
					Content: "a",
					User:    "root",
					Group:   "root",
					Mode:    "0664",
				}}, tasks.NewSimpleCond(&pb.Condition{})),
				tasks.NewManage([]*pb.ManagedFile{{
					Path:    "/tmp/a",
					Content: "a",
					User:    "root",
					Group:   "root",
					Mode:    "0664",
				}}, tasks.NewSimpleCond(&pb.Condition{}))},
		},
		want: "",
	}}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := tasks.CheckConflicts(tt.args.e, tt.args.install, tt.args.uninstall, tt.args.remove, tt.args.manage)
			if err != nil {
				assert.EqualError(t, err, tt.want)
			}
			if err == nil && tt.want != "" {
				t.Errorf("CheckConflicts return nil, want '%s' err", tt.want)
			}
		})
	}
}

func TestCheckFilesConflicts(t *testing.T) {
	type args struct {
		a *pb.ManagedFile
		b *pb.ManagedFile
	}
	tests := []struct {
		name string
		args args
		want string
	}{{
		name: "different files",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/b",
				Content: "b",
				User:    "vaspahomov",
				Group:   "vaspahomov",
				Mode:    "0777",
			},
		},
		want: "",
	}, {
		name: "same files different path",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/b",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
		},
		want: "",
	}, {
		name: "same files same path",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
		},
		want: "",
	}, {
		name: "same path different content",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "b",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
		},
		want: "different contents in same file '/tmp/a': 'a', 'b'",
	}, {
		name: "same path different user",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "vaspahomov",
				Group:   "root",
				Mode:    "0664",
			},
		},
		want: "different users for same file '/tmp/a': 'root', 'vaspahomov'",
	}, {
		name: "same path different group",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "vaspahomov",
				Mode:    "0664",
			},
		},
		want: "different groups for same file '/tmp/a': 'root', 'vaspahomov'",
	}, {
		name: "same path different mode",
		args: args{
			a: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0664",
			},
			b: &pb.ManagedFile{
				Path:    "/tmp/a",
				Content: "a",
				User:    "root",
				Group:   "root",
				Mode:    "0777",
			},
		},
		want: "different mode in same file '/tmp/a': '0664', '0777'",
	}}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tasks.CheckFilesConflicts(tt.args.a, tt.args.b); got != tt.want {
				t.Errorf("CheckFilesConflicts() = `%v`, want = `%v`", got, tt.want)
			}
		})
	}
}

func TestCheckPkgConflicts(t *testing.T) {
	type args struct {
		a *pb.SystemPackage
		b *pb.SystemPackage
	}
	tests := []struct {
		name string
		args args
		want string
	}{{
		name: "different packages",
		args: args{
			a: apt1,
			b: git,
		},
		want: "",
	}, {
		name: "same packages",
		args: args{
			a: apt1,
			b: apt1,
		},
		want: "",
	}, {
		name: "same packages different versions",
		args: args{
			a: apt1,
			b: apt2,
		},
		want: "different version in same pkg 'apt': '1.2.32ubuntu0.1', '1.2.32'",
	}}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tasks.CheckPkgConflicts(tt.args.a, tt.args.b); got != tt.want {
				t.Errorf("CheckPkgConflicts() = `%v`, want `%v`", got, tt.want)
			}
		})
	}
}
