package grafana

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/grafana-tools/sdk"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func getGrafanaCli() *sdk.Client {
	return sdk.NewClient(
		viper.GetString("GRAFANA_URL"),
		viper.GetString("GRAFANA_TOKEN"),
		sdk.DefaultHTTPClient,
	)
}

func TestClient_CreateAnnotation(t *testing.T) {
	test.InitTestEnv()

	gCli := getGrafanaCli()
	t.Cleanup(func() {
		err := clearTestAnnotation(gCli)
		require.NoError(t, err)
	})

	testTags := []string{
		"jenkins",
	}

	cli := NewClient()
	err := cli.CreateAnnotation("Master switched", testTags...)
	require.NoError(t, err)

	ctx := context.Background()
	ap := sdk.WithTag("drills-helper")
	annotations, err := gCli.GetAnnotations(ctx, ap)
	require.NoError(t, err)

	expectedTags := []string{
		"drills",
		"drills-helper",
	}
	expectedTags = append(expectedTags, testTags...)

	assert.ElementsMatch(t, expectedTags, annotations[0].Tags)
}

func TestClient_StartAnnotation(t *testing.T) {
	test.InitTestEnv()

	gCli := getGrafanaCli()
	t.Cleanup(func() {
		err := clearTestAnnotation(gCli)
		require.NoError(t, err)
	})

	testTags := []string{
		"sas",
		"jenkins",
	}

	cli := NewClient()
	err := cli.StartAnnotation("Deploy in jenkins disabled", testTags...)
	require.NoError(t, err)

	err = cli.EndAnnotation(testTags...)
	require.NoError(t, err)

	ctx := context.Background()
	ap := sdk.WithTag("drills-helper")
	annotations, err := gCli.GetAnnotations(ctx, ap)
	require.NoError(t, err)

	expectedTags := []string{
		"drills",
		"drills-helper",
	}
	expectedTags = append(expectedTags, testTags...)

	assert.ElementsMatch(t, expectedTags, annotations[0].Tags)
}

func TestClient_EndAnnotation(t *testing.T) {
	test.InitTestEnv()

	gCli := getGrafanaCli()
	t.Cleanup(func() {
		err := clearTestAnnotation(gCli)
		require.NoError(t, err)
	})

	// create annotation
	ar := sdk.CreateAnnotationRequest{}
	ar.Tags = []string{"inProgress", "drills", "drills-helper", "jenkins", "sas"}
	ar.Time = time.Now().UnixNano() / int64(time.Millisecond)
	ar.Text = "test"

	_, err := gCli.CreateAnnotation(context.Background(), ar)
	require.NoError(t, err)

	// try end annotation
	time.Sleep(5 * time.Second)
	cli := NewClient()
	err = cli.EndAnnotation("jenkins", "sas")
	require.NoError(t, err)

	// check that annotation is end
	ctx := context.Background()
	ap := sdk.WithTag("drills-helper")
	annotations, err := gCli.GetAnnotations(ctx, ap)
	require.NoError(t, err)

	require.NotEqual(t, annotations[0].Time, annotations[0].TimeEnd)
}

func clearTestAnnotation(cli *sdk.Client) error {
	ctx := context.Background()
	ap := sdk.WithTag("drills-helper")

	annotationsForDeletion, err := cli.GetAnnotations(ctx, ap)
	if err != nil {
		return fmt.Errorf("can't clear annotations %v", err)
	}
	for _, afd := range annotationsForDeletion {
		_, err = cli.DeleteAnnotation(ctx, afd.ID)
		if err != nil {
			return fmt.Errorf("can't clear annotations %v", err)
		}
	}
	return nil
}
