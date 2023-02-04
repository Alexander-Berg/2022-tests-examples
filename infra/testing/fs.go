package testing

import (
	"io"
	"sort"
	"testing"

	"a.yandex-team.ru/infra/hostctl/pkg/unitstorage"
	"github.com/stretchr/testify/assert"
)

func TestFSOpen(t *testing.T, fs unitstorage.FS, path, content string) {
	f, err := fs.Open(path)
	assert.NoError(t, err)
	defer f.Close()
	c, err := io.ReadAll(f)
	assert.NoError(t, err)
	assert.Equal(t, content, string(c))
}

type ExpectedFileInfo struct {
	Name  string
	IsDir bool
}

func TestFSReadDir(t *testing.T, fs unitstorage.FS, path string, expected []*ExpectedFileInfo) {
	sort.SliceStable(expected, func(i, j int) bool {
		return expected[i].Name < expected[j].Name
	})
	dentries, err := fs.ReadDir(path)
	assert.NoError(t, err)
	assert.Equal(t, len(expected), len(dentries))
	sort.SliceStable(dentries, func(i, j int) bool {
		return dentries[i].Name() < dentries[j].Name()
	})
	for i := range dentries {
		assert.Equal(t, expected[i].Name, dentries[i].Name())
		assert.Equal(t, expected[i].IsDir, dentries[i].IsDir())
	}
}

func TestFSStat(t *testing.T, fs unitstorage.FS, path string, expected *ExpectedFileInfo) {
	stat, err := fs.Stat(path)
	assert.NoError(t, err)
	assert.Equal(t, expected.IsDir, stat.IsDir())
	assert.Equal(t, expected.Name, stat.Name())
}
