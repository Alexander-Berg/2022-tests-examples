package aptly

import (
	"testing"

	"aptly/pkg/aptly/internal"
	"aptly/pkg/packages"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

var (
	testPackages = packages.Map{
		"gcc-9-base": "9.3.0-10ubuntu2",
	}
)

func TestAptly(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	if !c.Exists("CI") || !c.Bool("CI") {
		t.Skip("this test will be run only in CI")
	}

	Init(c, logger)

	m, err := internal.CreateMirror(
		"test",
		"focal",
		internal.WithArchitectures("amd64"),
		internal.WithFilter("gcc-9-base"),
	)
	require.NoError(t, err)

	err = m.Update()
	require.NoError(t, err)

	s, err := internal.CreateSnapshot("test", m)
	require.NoError(t, err)

	count, err := s.PackageCount()
	require.NoError(t, err)
	require.Equal(t, int64(1), count)

	pkgs, err := s.Packages()
	require.NoError(t, err)
	require.Equal(t, testPackages, pkgs)

	err = s.Publish("s3:aptly:tests")
	require.NoError(t, err)
}
