package testing

import (
	"io"
	"sort"
	"testing"

	"a.yandex-team.ru/infra/hostctl/pkg/unitstorage"
	"github.com/stretchr/testify/assert"
)

func TestStorageDiscoverUnits(t *testing.T, s unitstorage.Storage, expectedUnits []string) {
	sort.SliceStable(expectedUnits, func(i, j int) bool {
		return expectedUnits[i] < expectedUnits[j]
	})
	units, err := s.DiscoverUnits()
	assert.NoError(t, err)
	sort.SliceStable(units, func(i, j int) bool {
		return units[i] < units[j]
	})
	assert.Equal(t, expectedUnits, units)
}

func TestStorageOpenFile(t *testing.T, s unitstorage.Storage, name string, expected *unitstorage.File, content string) {
	f, err := s.OpenFile(name)
	assert.NoError(t, err)
	defer f.Reader.Close()
	assert.Equal(t, expected.Path, f.Path)
	assert.Equal(t, expected.Repo, f.Repo)
	buf, err := io.ReadAll(f.Reader)
	assert.NoError(t, err)
	assert.Equal(t, content, string(buf))
}
