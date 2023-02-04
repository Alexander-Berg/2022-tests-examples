package tests

import (
	"fmt"
	"os"
	"path/filepath"
	"testing"

	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
	"a.yandex-team.ru/infra/hostctl/pkg/unitstorage"
	"github.com/stretchr/testify/assert"
)

const testPath = "fsopentest"

func TestLocalRootFS(t *testing.T) {
	// setup test env
	_ = os.RemoveAll(testPath)
	err := os.Mkdir(testPath, os.ModeDir|0o755)
	assert.NoError(t, err)
	f, err := os.OpenFile(fmt.Sprintf("%s/f1", testPath), os.O_CREATE|os.O_RDWR, 0o644)
	assert.NoError(t, err)
	_, err = f.Write([]byte("mock"))
	assert.NoError(t, err)
	assert.NoError(t, f.Close())
	absPath, err := filepath.Abs(testPath)
	assert.NoError(t, err)
	lrfs := unitstorage.NewLocalFS()

	// test Open
	absFilePath := fmt.Sprintf("%s/f1", absPath)
	ttesting.TestFSOpen(t, lrfs, absFilePath, "mock")

	// test ReadDir
	ttesting.TestFSReadDir(t, lrfs, absPath, []*ttesting.ExpectedFileInfo{{Name: "f1"}})

	// test Stat
	ttesting.TestFSStat(t, lrfs, absFilePath, &ttesting.ExpectedFileInfo{Name: "f1"})
	ttesting.TestFSStat(t, lrfs, testPath, &ttesting.ExpectedFileInfo{Name: testPath, IsDir: true})

	// teardown test env
	_ = os.RemoveAll(testPath)
}
