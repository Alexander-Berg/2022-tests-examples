package registry

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestApi_GetManifest(t *testing.T) {
	test.InitTestEnv()
	api := NewApi(NewConf(), test.NewLogger(t))
	info, err := api.GetImageConfig("shiva-test", "0.0.17")
	require.NoError(t, err)
	assert.Equal(t, []string{"/shiva_test"}, info.Cmd)
}

func TestApi_CheckImageExists_Found(t *testing.T) {
	test.InitTestEnv()

	api := NewApi(NewConf(), test.NewLogger(t))
	err := api.CheckImageExists("shiva-test", "0.0.17")
	test.Check(t, err)
}

func TestApi_CheckImageExists_NotFound(t *testing.T) {
	test.InitTestEnv()

	api := NewApi(NewConf(), test.NewLogger(t))
	err := api.CheckImageExists("shiva-test", "0.0.42.42.42")
	assert.True(t, errors.Is(err, common.ErrNotFound))
}
