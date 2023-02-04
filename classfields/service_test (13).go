package syncComputeInstances

import (
	"context"
	"github.com/YandexClassifieds/go-cloud-functions/pkg/compute"
	"github.com/spf13/viper"
	"testing"
	"time"
)

func TestRun(t *testing.T) {
	conductorGroup := "vertis_parking"

	viper.AutomaticEnv()
	viper.Set("FOLDER_ID", "test_folder")
	viper.Set("CONDUCTOR_GROUPS", conductorGroup)
	viper.Set("DOWNTIME_DURATION", 1*time.Minute)

	ctx := context.TODO()

	var cloudAnswerWithFakeInstances = func(ctx context.Context, folderId string) ([]compute.Instance, error) {
		return []compute.Instance{
			{
				Fqdn:  "docker-autotest-1-myt.test.vertis.yandex.net",
				Group: conductorGroup,
				Dc:    "myt",
			},
			{
				Fqdn:  "docker-autotest-2-myt.test.vertis.yandex.net",
				Group: conductorGroup,
				Dc:    "myt",
			},
		}, nil
	}

	var cloudAnswerWithoutInstances = func(ctx context.Context, folderId string) ([]compute.Instance, error) {
		return []compute.Instance{}, nil
	}

	// test and create
	computeGetCloudInstances = cloudAnswerWithFakeInstances
	err := Run(ctx)
	if err != nil {
		t.Error(err)
	}

	// wait for conductor O_o
	time.Sleep(5 * time.Second)

	// test and clean conductor
	computeGetCloudInstances = cloudAnswerWithoutInstances
	err = Run(ctx)
	if err != nil {
		t.Error(err)
	}
}
