package config

import (
	"testing"

	"github.com/YandexClassifieds/cms/common"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
)

func TestLayer(t *testing.T) {
	viper.Set("_DEPLOY_LAYER", "prod")
	layer := Layer()
	require.Equal(t, layer, common.Prod)

	viper.Set("_DEPLOY_LAYER", "")
	layer = Layer()
	require.Equal(t, layer, common.Unknown)
}
