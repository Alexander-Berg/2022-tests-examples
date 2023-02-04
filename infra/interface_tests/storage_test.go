package tests

import (
	"testing"

	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
	"a.yandex-team.ru/infra/hostctl/pkg/unitstorage"
	"github.com/stretchr/testify/assert"
)

func TestFSStorage_OpenFile(t *testing.T) {
	f := unitstorage.NewMemFS([]*unitstorage.MemFile{
		{
			Path:    "/var/lib/ya-salt/repo/current/core/units.d/apt.yaml",
			Content: "apt repo",
			Dir:     false,
		},
	})
	s, err := unitstorage.NewDefaultFSStorage(f, "/var/lib/ya-salt/repo/current", nil)
	assert.NoError(t, err)
	ttesting.TestStorageOpenFile(t, s, "apt.yaml",
		&unitstorage.File{
			Repo: "core",
			Path: "/var/lib/ya-salt/repo/current/core/units.d/apt.yaml",
		}, "apt repo",
	)
}

func TestFSStorage_DiscoverUnits(t *testing.T) {
	f := unitstorage.NewMemFS([]*unitstorage.MemFile{
		{
			Path:    "/var/lib/ya-salt/repo/current/core/units.d/apt.yaml",
			Content: "apt repo",
			Dir:     false,
		},
		{
			Path:    "/etc/hostman/units.d/apt.yaml",
			Content: "apt etc",
			Dir:     false,
		},
		{
			Path:    "/var/lib/ya-salt/repo/current/sysdev/porto-daemons.d/yandex-hbf-agent.yaml",
			Content: "hbf repo",
			Dir:     false,
		},
		{
			Path:    "/home/loadbase/ssh.yaml",
			Content: "ssh loadbase",
			Dir:     false,
		},
	})
	s, err := unitstorage.NewDefaultFSStorage(f, "/var/lib/ya-salt/repo/current", []string{"/home/loadbase"})
	assert.NoError(t, err)
	ttesting.TestStorageDiscoverUnits(t, s, []string{
		"apt", "yandex-hbf-agent", "ssh",
	})
}
