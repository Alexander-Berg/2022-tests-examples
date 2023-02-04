package app

import (
	"math/rand"
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/github-updater/app/storage"
	"github.com/YandexClassifieds/shiva/pkg/arc/file"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/stretchr/testify/require"
)

type fileState int

const (
	New fileState = iota
	Updated
	Deleted
	NotChanged
)

const (
	actualRevision uint64 = 10
	knownRevision         = 8
)

type fileSystem struct {
	t *testing.T

	confService     *include.Service
	mapService      *service_map.Service
	manifestService *manifest.Service

	files map[string][]fileInfo
}

func newFileSystem(t *testing.T, confS *include.Service, mapS *service_map.Service, manifestS *manifest.Service) *fileSystem {
	return &fileSystem{
		t:               t,
		confService:     confS,
		mapService:      mapS,
		manifestService: manifestS,
		files:           map[string][]fileInfo{},
	}
}
func (s *fileSystem) addFile(path string, revision uint64, state fileState, data string) {
	basePath := basePathFromPath(path)

	f := fileInfo{
		path:     path,
		revision: revision,
		state:    state,
		data:     data,
	}

	if state == Deleted {
		s.save(f, f.revision)
		return
	}

	s.files[basePath] = append(s.files[basePath], f)
}

func (s *fileSystem) prepareMock(arcMock *mock.ArcFileService) {
	for basePath, files := range s.files {
		var filesInfo []*file.FileInfo
		for _, f := range files {
			filesInfo = append(filesInfo, &file.FileInfo{
				Name:                nameFromPath(f.path),
				RevisionLastChanged: f.revision,
			})
			if f.state == New || f.state == Updated || f.state == NotChanged {
				arcMock.On("ReadFile", actualRevision, f.path).Return([]byte(f.data), nil)
			}
			if f.state == Updated {
				s.save(f, uint64(rand.Int31n(knownRevision))+1)
			}
			if f.state == NotChanged {
				s.save(f, f.revision)
			}
		}
		arcMock.On("ListFiles", actualRevision, basePath).Return(filesInfo, nil)
	}
}

func (s *fileSystem) save(f fileInfo, revision uint64) {
	itemStore := s.getItemStorage(basePathFromPath(f.path))
	require.NoError(s.t, itemStore.ReadAndSave([]byte(f.data), revision, f.path))
}

func (s *fileSystem) getItemStorage(basePath string) storage.ItemStore {
	switch basePath {
	case "maps":
		return s.mapService
	case "deploy":
		return s.manifestService
	case "conf":
		return s.confService
	}
	return nil
}

func basePathFromPath(path string) string {
	return strings.Split(path, "/")[0]
}
func nameFromPath(path string) string {
	basePath := basePathFromPath(path)
	return strings.TrimPrefix(path, basePath+"/")
}

type fileInfo struct {
	path     string
	revision uint64
	state    fileState
	data     string
}
