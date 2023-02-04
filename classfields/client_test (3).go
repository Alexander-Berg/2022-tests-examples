package s3

import (
	"fmt"
	"math/rand"
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/stretchr/testify/require"
)

type testData struct {
	String string            `json:"string"`
	Slice  []string          `json:"slice"`
	Map    map[string]string `json:"map"`
}

func random() string {
	return fmt.Sprintf("rand-%d", rand.Int63())
}

func TestClient_PutGet(t *testing.T) {
	c := viper.NewTestConf()
	cli := New(c)

	defer func() {
		_, err := cli.sdk.DeleteObject(&s3.DeleteObjectInput{
			Bucket: aws.String(c.Str("S3_BUCKET")),
			Key:    aws.String(c.Str("S3_PREFIX") + "/test"),
		})
		require.NoError(t, err)
	}()

	putData := testData{
		String: random(),
		Slice:  []string{random(), random()},
		Map:    map[string]string{random(): random()},
	}

	require.NoError(t, cli.Put("test", putData))

	var getData testData
	require.NoError(t, cli.Get("test", &getData))
	require.Equal(t, putData, getData)
}
