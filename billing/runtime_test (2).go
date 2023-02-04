package memory

import (
	"testing"

	"github.com/stretchr/testify/suite"

	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
)

type MemoryBlockStorageTestSuite struct {
	storagetest.BlockStorageTestSuite
}

func (s *MemoryBlockStorageTestSuite) SetupTest() {
	s.BlockStorageTestSuite.SetupTest()
	s.Storage = NewRuntimeStorage(s.TemplateStorage)
}

func TestBlockStorage(t *testing.T) {
	suite.Run(t, new(MemoryBlockStorageTestSuite))
}

type MemoryGraphStorageTestSuite struct {
	storagetest.GraphStorageTestSuite
}

func (s *MemoryGraphStorageTestSuite) SetupSuite() {
	s.Storage = NewRuntimeStorage(nil)
}

func TestGraphStorageTestSuite(t *testing.T) {
	suite.Run(t, new(MemoryGraphStorageTestSuite))
}
