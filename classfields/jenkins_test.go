package jenkins

import (
	gojenkins "github.com/YandexClassifieds/go-jenkins-client"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestJobMatches(t *testing.T) {
	testJob1 := gojenkins.JobInfo{
		Job:  gojenkins.Job{Name: "yandex-vertis-autoparts-avito-parsing-api"},
		Path: "/job/Deploys/job/Parsing/job/testing/job/yandex-vertis-autoparts-avito-parsing-api/",
	}
	testJob2 := gojenkins.JobInfo{
		Job:  gojenkins.Job{Name: "parsing-api"},
		Path: "/job/Deploys/job/Parsing/job/prod/job/parsing-api/",
	}

	assert.Equal(t, jobMatches(testJob1, testJob2.Job.Name, "test"), false, "Incorrect jobMatches")
	assert.Equal(t, jobMatches(testJob2, testJob1.Job.Name, "prod"), false, "Incorrect jobMatches")
	assert.Equal(t, jobMatches(testJob2, testJob1.Job.Name, "test"), false, "Incorrect jobMatches")
	assert.Equal(t, jobMatches(testJob2, testJob2.Job.Name, "test"), false, "Incorrect jobMatches because of env settings")
	assert.Equal(t, jobMatches(testJob1, testJob1.Job.Name, "test"), true, "Incorrect jobMatches")
}
