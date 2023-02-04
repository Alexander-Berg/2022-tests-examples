package override

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/env_override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	serviceName = "testService"
	manifestYml = `
name: %s
test:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
prod:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
`
	manifestPath = "deploy/%s.yml"
)

func TestService_GetManifest_WithoutOverrides(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := makeService(t, db)

	dID := int64(1)
	mID := makeManifest(t, db, serviceName)

	m, err := s.GetManifest(common.Test, dID, mID)
	require.NoError(t, err)
	assert.Len(t, m.Config.OverrideParams, 0)
	assert.Len(t, m.Config.OverrideFiles, 0)
}

func TestService_GetManifest_WithOverrides(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := makeService(t, db)

	dID := int64(1)
	mID := makeManifest(t, db, serviceName)
	overridesEnvMap := map[string]string{"TEST_KEY": "TEST_VALUE"}
	makeEnvOverrides(t, db, dID, overridesEnvMap)
	makeIncludeOverrides(t, db, dID, map[string]string{
		"shiva/override.yml": `TEST_PARAM: test param`,
	})

	m, err := s.GetManifest(common.Test, dID, mID)
	require.NoError(t, err)
	assert.Equal(t, overridesEnvMap, m.Config.OverrideParams)
	assert.Len(t, m.Config.OverrideFiles, 1)
	assert.Equal(t, "shiva/override.yml", m.Config.OverrideFiles[0].Path)
}

func makeManifest(t *testing.T, db *storage.Database, name string) *manifest.Data {
	log := test.NewLogger(t)
	svc := manifest.NewService(db, log, parser.NewService(log, nil), include.NewService(db, log))
	require.NoError(t, svc.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf(manifestPath, name)))

	data := &manifest.Data{}
	err := db.GormDb.Where("name = ?", name).Take(data).Error
	require.NoError(t, err)
	return data
}

func makeEnvOverrides(t *testing.T, db *storage.Database, deploymentID int64, envOverride map[string]string) {
	envOverrideStore := env_override.NewStorage(db, test.NewLogger(t))
	var deploymentEnvOverride []*env_override.Model
	for key, value := range envOverride {
		deploymentEnvOverride = append(deploymentEnvOverride, &env_override.Model{
			DeploymentId: deploymentID,
			Key:          key,
			Value:        value,
		})
	}
	err := envOverrideStore.Save(deploymentEnvOverride)
	require.NoError(t, err)
}

func makeIncludeOverrides(t *testing.T, db *storage.Database, deploymentID int64, includeOverride map[string]string) {
	var incs []*domain.Include
	for path, file := range includeOverride {
		incs = append(incs, makeInclude(t, db, file, path))
	}

	includeLinksStore := include_links.NewStorage(db, test.NewLogger(t))
	var deploymentIncludes []*include_links.DeploymentIncludes
	for _, inc := range incs {
		deploymentIncludes = append(deploymentIncludes, &include_links.DeploymentIncludes{
			DeploymentId: deploymentID,
			IncludeId:    inc.ID(),
			Override:     true,
		})
	}
	err := includeLinksStore.Save(deploymentIncludes)
	require.NoError(t, err)
}

func makeInclude(t *testing.T, db *storage.Database, includeFile, path string) *domain.Include {
	service := include.NewService(db, test.NewLogger(t))
	err := service.ReadAndSave([]byte(includeFile), 10, path)
	require.NoError(t, err)
	inc, err := service.GetByPath(path)
	require.NoError(t, err)
	return inc
}

func makeService(t *testing.T, db *storage.Database) *Service {
	log := test.NewLogger(t)
	includeSrv := include.NewService(db, log)
	manifestSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)

	return NewService(db, log, includeSrv, manifestSrv)
}
