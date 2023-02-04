package static

import (
	_ "embed"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"testing"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v3"
)

func TestParseFile1(t *testing.T) {
	tmpDir := t.TempDir()
	testStaticClusters := &Clusters{}
	err := json.Unmarshal(testJson, testStaticClusters)
	if err != nil {
		t.Errorf("Can't Unmarshal test json %s: '%s'", testJson, err)
	}

	newStaticConfigFile := tmpDir + "/test1_clusters_static_config.json"
	createTestStaticFile(newStaticConfigFile, testJson)

	yamlFile := tmpDir + "/test.yaml"
	require.NoError(t, ioutil.WriteFile(yamlFile, staticData, 0755))

	s := NewService(logger.Logger, Conf{FilePath: newStaticConfigFile, YamlConfigPath: yamlFile})
	if newStaticConfigFile != s.conf.FilePath {
		t.Errorf("Failed to use newStaticConfigFile variable for Init")
	}
	staticClusters := s.StaticClusters()
	assert.Equal(t, testStaticClusters, staticClusters)

	yamlCfg := &ExtraConfiguration{}
	require.NoError(t, yaml.Unmarshal(staticData, yamlCfg))
	assert.Equal(t, yamlCfg, s.Data())
}

func TestParseFile2(t *testing.T) {
	s := NewService(logger.Logger, NewConf())
	assert.Empty(t, s.StaticClusters())
}

func createTestStaticFile(filename string, testJson []byte) {
	err := ioutil.WriteFile(filename, testJson, 0755)
	if err != nil {
		panic(fmt.Sprintf("Can't write file '%s' for test CreateTestStaticFile", err))
	}
}

var (
	//go:embed test_config.json
	testJson []byte
)
