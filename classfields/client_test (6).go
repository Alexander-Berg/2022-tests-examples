package jenkins

import (
	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test"
	gojenkins "github.com/YandexClassifieds/go-jenkins-client"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
	"time"
)

func getJenkins() (string, *gojenkins.Jenkins) {
	auth := &gojenkins.Auth{
		Username: viper.GetString("JENKINS_USER"),
		ApiToken: viper.GetString("JENKINS_PASSWORD"),
	}
	return viper.GetString("JENKINS_JOB_NAME"), gojenkins.NewJenkins(auth, viper.GetString("JENKINS_ENDPOINT"))
}

func TestJenkins_DeployEnable(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	jenkins := New()
	err := jenkins.Enable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	// Wait for update last job id
	time.Sleep(5 * time.Second)

	jobName, client := getJenkins()
	out, err := getJobSwitchOutput(jobName, client)
	require.NoError(t, err)

	require.Contains(t, out, "Disabled: false for job generator")
}

func TestJenkins_DeployDisable(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	jenkins := New()
	err := jenkins.Disable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	// Wait for update last job id
	time.Sleep(5 * time.Second)

	jobName, client := getJenkins()
	out, err := getJobSwitchOutput(jobName, client)
	require.NoError(t, err)

	require.Contains(t, out, "Disabled: true for job generator")
}

func getJobSwitchOutput(jobName string, client *gojenkins.Jenkins) (string, error) {
	job := gojenkins.Job{}
	job.Name = jobName

	build, err := client.GetLastBuild(job)
	if err != nil {
		return "", err
	}

	for build.Building {
		build, err = client.GetLastBuild(job)
		if err != nil {
			return "", err
		}
	}

	out, err := client.GetBuildConsoleOutput(build)
	if err != nil {
		return "", err
	}
	return string(out), nil
}
