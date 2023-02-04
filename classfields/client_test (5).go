package conductor

import (
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"net/http"
	"os"
	"testing"
)

func conductorGetCurrentStatus() ([]byte, error) {
	url := fmt.Sprintf("%s/api/v2/projects/%s",
		viper.GetString("CONDUCTOR_ENDPOINT"), viper.GetString("CONDUCTOR_PROJECT"))

	return common.HTTPOAuthWithContentType(&http.Client{}, http.MethodGet, url, nil, viper.GetString("DH_OAUTH_TOKEN"), "application/json")
}

func TestConductor_DeployDisableProd(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	conductor := New()
	err := conductor.Disable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	content, err := conductorGetCurrentStatus()
	require.NoError(t, err)

	var ds CurrentDeploySchedule
	err = json.Unmarshal(content, &ds)
	require.NoError(t, err)

	if ds.Data.Attributes.DeploySchedule.Stable.Autoinstall || ds.Data.Attributes.DeploySchedule.Prestable.Autoinstall {
		t.Fatal("after the test, the deploy should have been turned on")
	}
}

func TestConductor_DeployDisableTest(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	conductor := New()
	err := conductor.Disable(common.LayerTest, common.DCSas)
	require.NoError(t, err)

	content, err := conductorGetCurrentStatus()
	require.NoError(t, err)

	var ds CurrentDeploySchedule
	err = json.Unmarshal(content, &ds)
	require.NoError(t, err)

	if ds.Data.Attributes.DeploySchedule.Unstable.Autoinstall || ds.Data.Attributes.DeploySchedule.Testing.Autoinstall {
		t.Fatal("after the test, the deploy should have been turned on")
	}
}

func TestConductor_DeployEnableProd(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	conductor := New()
	err := conductor.Enable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	content, err := conductorGetCurrentStatus()
	require.NoError(t, err)

	var ds CurrentDeploySchedule
	err = json.Unmarshal(content, &ds)
	require.NoError(t, err)

	if !ds.Data.Attributes.DeploySchedule.Stable.Autoinstall || !ds.Data.Attributes.DeploySchedule.Prestable.Autoinstall {
		t.Fatal("after the test, the deploy should have been turned on")
	}
}

func TestConductor_DeployEnableTest(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	conductor := New()
	err := conductor.Enable(common.LayerTest, common.DCSas)
	require.NoError(t, err)

	content, err := conductorGetCurrentStatus()
	require.NoError(t, err)

	var ds CurrentDeploySchedule
	err = json.Unmarshal(content, &ds)
	require.NoError(t, err)

	if !ds.Data.Attributes.DeploySchedule.Unstable.Autoinstall || !ds.Data.Attributes.DeploySchedule.Testing.Autoinstall {
		t.Fatal("after the test, the deploy should have been turned on")
	}
}

func TestConductor_MakeDeploySchedule(t *testing.T) {
	test.InitTestEnv()

	conductor := Client{
		cli:     &http.Client{},
		baseURL: viper.GetString("CONDUCTOR_ENDPOINT"),
		token:   viper.GetString("DH_OAUTH_TOKEN"),
		project: viper.GetString("CONDUCTOR_PROJECT"),
	}

	ds, err := conductor.makeDeploySchedule(common.LayerProd, true)
	require.NoError(t, err)

	require.True(t, ds.DeploySchedule.Stable.Autoinstall)
	require.True(t, ds.DeploySchedule.Prestable.Autoinstall)

	ds, err = conductor.makeDeploySchedule(common.LayerProd, false)
	require.NoError(t, err)

	require.False(t, ds.DeploySchedule.Stable.Autoinstall)
	require.False(t, ds.DeploySchedule.Prestable.Autoinstall)

	ds, err = conductor.makeDeploySchedule(common.LayerTest, true)
	require.NoError(t, err)

	require.True(t, ds.DeploySchedule.Unstable.Autoinstall)
	require.True(t, ds.DeploySchedule.Testing.Autoinstall)

	ds, err = conductor.makeDeploySchedule(common.LayerTest, false)
	require.NoError(t, err)

	require.False(t, ds.DeploySchedule.Unstable.Autoinstall)
	require.False(t, ds.DeploySchedule.Testing.Autoinstall)
}
