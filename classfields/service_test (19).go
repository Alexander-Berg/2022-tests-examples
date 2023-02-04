package shiva

import (
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

func TestService_GetPciDssServices(t *testing.T) {
	c := viper.NewTestConf()
	l := logrus.NewLogger()

	s := NewService(c, l)
	services, err := s.GetPciDssServices()
	require.NoError(t, err)
	require.Contains(t, services, "shiva-test-pcidss")
}
