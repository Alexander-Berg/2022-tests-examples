package s3

import (
	"crypto/md5"
	"fmt"
	"github.com/YandexClassifieds/shiva/common/config/size"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/aws/aws-sdk-go/aws"
	awsS3 "github.com/aws/aws-sdk-go/service/s3"
	"github.com/stretchr/testify/require"
	"io"
	"io/ioutil"
	"math/rand"
	"path/filepath"
	"testing"
	"time"
)

func TestUploadObject(t *testing.T) {
	test.InitTestEnv()
	rand.Seed(time.Now().UnixNano())
	log := test.NewLogger(t)
	filePath := filepath.Join(t.TempDir(), "test.hprof")
	expectedData := make([]byte, 101*size.MB)
	rand.Read(expectedData)
	err := ioutil.WriteFile(
		filePath,
		expectedData,
		0644)
	if err != nil {
		t.Fatal(err)
	}
	conf := NewConf()
	s3client := NewS3Client(log, conf)
	s := NewService(log, conf, s3client)

	err = s.doUpload(filePath)

	require.NoError(t, err)
	res, err := s3client.GetObject(&awsS3.GetObjectInput{
		Bucket: aws.String(conf.bucket),
		Key:    aws.String(fmt.Sprintf("%s/%s", conf.directory, filepath.Base(filePath))),
	})
	require.NoError(t, err)
	require.Equal(t, int64(len(expectedData)), *res.ContentLength)
	actualData := make([]byte, *res.ContentLength)
	_, err = io.ReadAtLeast(res.Body, actualData, len(expectedData))
	require.NoError(t, err)
	require.Equal(t, md5.Sum(expectedData), md5.Sum(actualData))
}
