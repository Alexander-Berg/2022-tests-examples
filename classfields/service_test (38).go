package reader

import (
	"fmt"
	"sort"
	"strconv"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/common"
	sm "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	envTypes "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestResolveTvmID(t *testing.T) {
	s, sMap := makeServices(t)
	prepareEnvs(t, s, sMap)

	tvm, err := s.ResolveTVM("v1")
	require.NoError(t, err)
	assert.Equal(t, "s1", tvm.Service)
	assert.Equal(t, common.Prod, tvm.Layer)

	tvm, err = s.ResolveTVM("v2")
	require.NoError(t, err)
	assert.Equal(t, "s2", tvm.Service)
	assert.Equal(t, common.Test, tvm.Layer)
}

func TestResolveTvmID_NotFoundError(t *testing.T) {
	s, sMap := makeServices(t)
	prepareEnvs(t, s, sMap)

	_, err := s.ResolveTVM("v3")
	assert.Equal(t, common.ErrNotFound, err)

	_, err = s.ResolveTVM("0")
	assert.Equal(t, common.ErrNotFound, err)
}

func prepareEnvs(t *testing.T, reader *Service, sMap *service_map.Service) {
	require.NoError(t, reader.storage.Save(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_ID, "s1", 1)))
	require.NoError(t, reader.storage.Save(newEnv(common.Test, envTypes.EnvType_GENERATED_TVM_ID, "s2", 2)))
	require.NoError(t, reader.storage.Save(newEnv(common.Test, envTypes.EnvType_GENERATED_TVM_SECRET, "s2", 3)))
	require.NoError(t, reader.storage.Save(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_ID, "s2", 4)))
	require.NoError(t, reader.storage.Save(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_SECRET, "s2", 5)))
	require.NoError(t, reader.storage.Save(newEnv(common.Unknown, envTypes.EnvType_GENERATED_TVM_ID, "s2", 6)))

	makeSimpleMap(t, sMap, "s1", sm.ServiceType_service.String())
	makeSimpleMap(t, sMap, "s2", sm.ServiceType_service.String())
	makeSimpleMap(t, sMap, "s3", sm.ServiceType_service.String())
	makeSimpleMap(t, sMap, "s4", sm.ServiceType_service.String())
	makeSimpleMap(t, sMap, "s5", sm.ServiceType_service.String())
	makeSimpleMap(t, sMap, "s6", sm.ServiceType_service.String())
}

func TestGet(t *testing.T) {
	type TestCase struct {
		service  []string
		layer    []common.Layer
		envTypes []envTypes.EnvType
		result   []int
	}
	s, sMap := makeServices(t)
	prepareEnvs(t, s, sMap)
	cases := []TestCase{
		{
			result: []int{1, 2, 3, 4, 5, 6},
		},
		{
			layer:  []common.Layer{common.Prod},
			result: []int{1, 4, 5, 6},
		},
		{
			service: []string{"s2"},
			layer:   []common.Layer{common.Prod},
			result:  []int{4, 5, 6},
		},
		{
			service: []string{"s1"},
			layer:   []common.Layer{common.Prod},
			result:  []int{1},
		},
		{
			envTypes: []envTypes.EnvType{envTypes.EnvType_GENERATED_TVM_ID},
			layer:    []common.Layer{common.Prod},
			result:   []int{1, 4, 6},
		},
		{
			service:  []string{"s2"},
			envTypes: []envTypes.EnvType{envTypes.EnvType_GENERATED_TVM_SECRET},
			layer:    []common.Layer{common.Prod},
			result:   []int{5},
		},
	}
	for _, c := range cases {
		t.Run("GetAll", func(t *testing.T) {
			result, err := s.All(c.envTypes, c.layer, c.service)
			require.NoError(t, err)
			var resultKeys []int
			for _, m := range result {
				n, err := strconv.Atoi(m.Key)
				require.NoError(t, err)
				resultKeys = append(resultKeys, n)
			}
			require.Len(t, resultKeys, len(c.result))
			sort.Ints(c.result)
			sort.Ints(resultKeys)
			for i := range resultKeys {
				assert.Equal(t, resultKeys[i], c.result[i])
			}
		})
	}
}

func TestEnvMap(t *testing.T) {
	type TestCase struct {
		service string
		layer   common.Layer
		result  []int
	}
	cases := []TestCase{
		{
			service: "s1",
			layer:   common.Prod,
			result:  []int{1},
		},
		{
			service: "s1",
			layer:   common.Test,
			result:  []int{},
		},
		{
			service: "s2",
			layer:   common.Prod,
			result:  []int{4, 5, 6},
		},
		{
			service: "s2",
			layer:   common.Test,
			result:  []int{2, 3, 6},
		},
	}
	s, sMap := makeServices(t)
	prepareEnvs(t, s, sMap)
	for _, c := range cases {
		t.Run("EnvMap", func(t *testing.T) {
			result, err := s.EnvMap(c.layer, c.service)
			require.NoError(t, err)
			require.Len(t, result, len(c.result))
			for _, key := range c.result {
				_, ok := result[strconv.Itoa(key)]
				assert.True(t, ok, key)
			}
		})
	}
}

func newEnv(l common.Layer, t envTypes.EnvType, s string, n int) *storage.ExternalEnv {
	strI := strconv.Itoa(n)
	return &storage.ExternalEnv{
		Service: s,
		Layer:   l,
		Type:    t,
		Key:     strI,
		Value:   "v" + strI,
	}
}

func makeServices(t *testing.T) (*Service, *service_map.Service) {
	test.RunUp(t)
	defer test.Down(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	sMap := service_map.NewService(db, log, service_change.NewNotificationMock())
	return NewService(db, log), sMap
}

func makeSimpleMap(t *testing.T, s *service_map.Service, name, sType string) {
	mapStr := fmt.Sprintf(`
name: %s
type: %s`, name, sType)
	path := fmt.Sprintf("maps/%s.yml", name)
	require.NoError(t, s.ReadAndSave([]byte(mapStr), test.AtomicNextUint(), path))
}
