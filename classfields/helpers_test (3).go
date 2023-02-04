package teamcity_event_handler

import (
	"errors"
	"fmt"
	"github.com/YandexClassifieds/hub/config"
	sf "github.com/YandexClassifieds/hub/standard_functions"
	"net/url"
	"testing"
)

func checkBuildShouldBeAddedResult(t *testing.T, buildPath string, affectedFiles []string, expectedResult bool) {
	if buildShouldBeAdded(buildPath, affectedFiles) != expectedResult {
		t.Errorf("Failed to check whether build should be added for build path '%s', affected files '%v': got %t, "+
			"expected %t", buildPath, affectedFiles, !expectedResult, expectedResult)
	}
}

func TestBuildShouldBeAdded(t *testing.T) {
	affectedPaths := []string{
		"Gopkg.lock",
		"Gopkg.toml",
		"cluster/cluster.go",
		"consul/cache.go consul/consul.go",
		"consul/consul_scheduler.go",
		"logger/logger.go",
		"main.go",
		"parsing/parsing.go",
		"route/route.go",
		"slbcheck/slbcheck.go",
		"static/clusters_static_config.json",
		"static/static.go",
		"utils/utils.go",
		"vendor/github.com/gorilla/context/.travis.yml",
		"vendor/github.com/gorilla/context/LICENSE",
		"vendor/github.com/gorilla/context/README.md",
		"vendor/github.com/gorilla/context/context.go",
		"vendor/github.com/gorilla/context/context_test.go",
		"webhooks/github_event_handlers/startrek_event_handler.go"}

	checkBuildShouldBeAddedResult(t, "consul/*", affectedPaths, true)
	checkBuildShouldBeAddedResult(t, "*", affectedPaths, true)
	checkBuildShouldBeAddedResult(t, "", affectedPaths, true)
	checkBuildShouldBeAddedResult(t, "cluster/*", affectedPaths, true)
	checkBuildShouldBeAddedResult(t, "clusters/*", affectedPaths, false)
	checkBuildShouldBeAddedResult(t, "context/*", affectedPaths, false)
	checkBuildShouldBeAddedResult(t, "vendor/github.com/gorilla*", affectedPaths, true)
	checkBuildShouldBeAddedResult(t, `static/*.json`, affectedPaths, true)
	checkBuildShouldBeAddedResult(t, `vendor/*.json`, affectedPaths, false)
	checkBuildShouldBeAddedResult(t, `*README.md`, affectedPaths, true)
	checkBuildShouldBeAddedResult(t, `webhooks/*`, affectedPaths, true)
}

func checkParseBuildsFromUrl(t *testing.T, values url.Values, expectedBuildTypeIds []string, expectedBuildProperties BuildTypeToBuildProperties, expectedError error) {
	buildTypeIds, buildProperties, err := parseBuildsFromUrl(values)
	if !sf.TestEqualErrors(err, expectedError) {
		t.Errorf("Failed to parse builds from url for values '%v': got error '%v', "+
			"expected '%v'", values, err, expectedError)
	}

	if !sf.TestEqualStringArray(buildTypeIds, expectedBuildTypeIds) {
		t.Errorf("Failed to parse builds from url for values '%v': got build type ids '%v', "+
			"expected '%v'", values, buildTypeIds, expectedBuildTypeIds)
	}

	if !sf.TestEqualStringDoubleMap(buildProperties, expectedBuildProperties) {
		t.Errorf("Failed to parse builds from url for values '%v': got build properties '%v', "+
			"expected '%v'", values, buildProperties, expectedBuildProperties)
	}
}

func checkParseBuildsFromConfig(t *testing.T, hubConfig config.Config, affectedFiles []string, expectedBuildTypeIds []string, expectedBuildProperties BuildTypeToBuildProperties, expectedError error) {
	buildTypeIds, buildProperties, err := parseBuildsFromConfig(hubConfig, affectedFiles)
	if !sf.TestEqualErrors(err, expectedError) {
		t.Errorf("Failed to parse builds from url for config '%v', affectedFiles '%v': got error '%v', "+
			"expected '%v'", hubConfig, affectedFiles, err, expectedError)
	}
	if err != nil {
		return
	}

	if !sf.TestEqualStringArray(buildTypeIds, expectedBuildTypeIds) {
		t.Errorf("Failed to parse builds from url for config '%v', affectedFiles '%v': got build type ids '%v', "+
			"expected '%v'", hubConfig, affectedFiles, buildTypeIds, expectedBuildTypeIds)
	}

	if !sf.TestEqualStringDoubleMap(buildProperties, expectedBuildProperties) {
		t.Errorf("Failed to parse builds from url for config '%v', affectedFiles '%v': got build properties '%v', "+
			"expected '%v'", hubConfig, affectedFiles, buildProperties, expectedBuildProperties)
	}
}

func TestParseBuildFromUrl(t *testing.T) {
	// valid params
	params1 := url.Values{}
	params1.Set(TeamcityBuildTypeIdQueryName, "HubBuild,HprofIntermediaryBuild")
	params1.Set("name", "Nastia")
	params1.Set("city", "Spb")
	expectedBuildTypeIds1 := []string{"HubBuild", "HprofIntermediaryBuild"}
	expectedBuildProperties1 := make(BuildTypeToBuildProperties, 0)
	expectedBuildProperties1["HubBuild"] = BuildPropertyNameToValue{"name": "Nastia", "city": "Spb"}
	expectedBuildProperties1["HprofIntermediaryBuild"] = BuildPropertyNameToValue{"name": "Nastia", "city": "Spb"}

	checkParseBuildsFromUrl(t, params1, expectedBuildTypeIds1, expectedBuildProperties1, nil)

	// params without build type id
	params2 := url.Values{}
	params2.Set("name", "Nastia")
	params2.Set("city", "Spb")
	expectedBuildTypeIds2 := make([]string, 0)
	expectedBuildProperties2 := make(BuildTypeToBuildProperties, 0)
	err2 := errors.New("cannot trigger teamcity build via url params - webhook doesn't have build type id.")

	checkParseBuildsFromUrl(t, params2, expectedBuildTypeIds2, expectedBuildProperties2, err2)
}

func TestParseBuildFromConfig(t *testing.T) {
	affectedFiles := []string{
		"Gopkg.lock",
		"Gopkg.toml",
		"cluster/cluster.go",
		"consul/cache.go consul/consul.go",
		"consul/consul_scheduler.go",
		"logger/logger.go",
		"main.go",
		"parsing/parsing.go",
		"route/route.go",
		"slbcheck/slbcheck.go",
		"static/clusters_static_config.json",
		"static/static.go",
		"utils/utils.go",
		"vendor/github.com/gorilla/context/.travis.yml",
		"vendor/github.com/gorilla/context/LICENSE",
		"vendor/github.com/gorilla/context/README.md",
		"vendor/github.com/gorilla/context/context.go",
		"vendor/github.com/gorilla/context/context_test.go"}

	// simple root path
	hubConfig := config.Config{}
	hubConfig.AllBuilds.TeamcityBuilds = append(hubConfig.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "HubBuild", Path: "*", BuildProperties: []config.Property{
		{Name: "name", Value: "Nastia"}, {Name: "city", Value: "Spb"}}})

	expectedBuildTypeIds1 := []string{"HubBuild"}
	expectedBuildProperties1 := make(BuildTypeToBuildProperties, 0)
	expectedBuildProperties1["HubBuild"] = BuildPropertyNameToValue{"name": "Nastia", "city": "Spb"}

	checkParseBuildsFromConfig(t, hubConfig, affectedFiles, expectedBuildTypeIds1, expectedBuildProperties1, nil)

	// multiple builds and paths
	hubConfig2 := config.Config{}
	hubConfig2.AllBuilds.TeamcityBuilds = append(hubConfig2.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "HubBuild", Path: "parsing/*", BuildProperties: []config.Property{
		{Name: "name", Value: "Nastia"}, {Name: "city", Value: "Spb"}}})
	hubConfig2.AllBuilds.TeamcityBuilds = append(hubConfig2.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "HprofIntermediaryBuild", Path: "vendor/github.com/gorilla/context*", BuildProperties: []config.Property{
		{Name: "name2", Value: "Mascha"}, {Name: "city2", Value: "Moscow"}}})
	hubConfig2.AllBuilds.TeamcityBuilds = append(hubConfig2.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "BuildThatShouldNotBeAdded", Path: "vendors*", BuildProperties: []config.Property{
		{Name: "name3", Value: "Kris"}, {Name: "city3", Value: "Moscow"}}})

	expectedBuildTypeIds2 := []string{"HubBuild", "HprofIntermediaryBuild"}
	expectedBuildProperties2 := make(BuildTypeToBuildProperties, 0)
	expectedBuildProperties2["HubBuild"] = BuildPropertyNameToValue{"name": "Nastia", "city": "Spb"}
	expectedBuildProperties2["HprofIntermediaryBuild"] = BuildPropertyNameToValue{"name2": "Mascha", "city2": "Moscow"}

	checkParseBuildsFromConfig(t, hubConfig2, affectedFiles, expectedBuildTypeIds2, expectedBuildProperties2, nil)

	// error: without builds
	hubConfig3 := config.Config{}
	expectedBuildTypeIds3 := []string{}
	expectedBuildProperties3 := make(BuildTypeToBuildProperties, 0)
	checkParseBuildsFromConfig(t, hubConfig3, affectedFiles, expectedBuildTypeIds3, expectedBuildProperties3,
		errors.New(fmt.Sprintf("cannot trigger teamcity build via hub config - config doesn't have any builds.")))

	// without builds names
	hubConfig4 := config.Config{}
	hubConfig4.AllBuilds.TeamcityBuilds = append(hubConfig4.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "HubBuild", Path: "parsing/*", BuildProperties: []config.Property{
		{Name: "name", Value: "Nastia"}, {Name: "city", Value: "Spb"}}})
	hubConfig4.AllBuilds.TeamcityBuilds = append(hubConfig4.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "", Path: "vendor/github.com/gorilla/context*", BuildProperties: []config.Property{
		{Name: "name2", Value: "Mascha"}, {Name: "city2", Value: "Moscow"}}})

	expectedBuildTypeIds4 := []string{"HubBuild"}
	expectedBuildProperties4 := make(BuildTypeToBuildProperties, 0)

	checkParseBuildsFromConfig(t, hubConfig4, affectedFiles, expectedBuildTypeIds4, expectedBuildProperties4,
		errors.New("cannot trigger teamcity build via hub config - build name couldn't be empty."))

	// without properties names
	hubConfig5 := config.Config{}
	hubConfig5.AllBuilds.TeamcityBuilds = append(hubConfig5.AllBuilds.TeamcityBuilds, config.TeamcityBuild{BuildTypeId: "HubBuild", Path: "parsing/*", BuildProperties: []config.Property{
		{Name: "", Value: "Nastia"}, {Name: "city", Value: "Spb"}}})

	expectedBuildTypeIds5 := []string{"HubBuild"}
	expectedBuildProperties5 := make(BuildTypeToBuildProperties, 0)

	checkParseBuildsFromConfig(t, hubConfig5, affectedFiles, expectedBuildTypeIds5, expectedBuildProperties5,
		errors.New("cannot trigger teamcity build via hub config - property name couldn't be empty."))
}

func TestGetLogicalBranch(t *testing.T) {
	tests := map[string]string{
		"refs/heads/feature":    "feature",
		"refs/heads/feature   ": "feature",
		"refs/heads/ feature ":  "",
		"refs/heads/feature 1 ": "feature",
		"refs/heads/":           "",
		"FOO":                   "",
		"":                      "",
		"refs/heads/    ":       "",
		"refs/pull/1/head":      "",
	}
	for ref, expectedBranch := range tests {
		branch := GetLogicalBranch(ref)
		if branch != expectedBranch {
			t.Errorf("%s: %q: got %q expected %q", ref, expectedBranch, branch, expectedBranch)
		}
	}
}

func TestIsTagBranch(t *testing.T) {
	tests := map[string]bool{
		"refs/heads/feature":     false,
		"refs/heads/feature   ":  false,
		"refs/heads/ feature ":   false,
		"refs/tags/foo":          true,
		"refs/tags":              false,
		"FOO":                    false,
		"":                       false,
		"refs/heads/    ":        false,
		"refs/pull/1/head":       false,
		"refs/tags/hub-api_0.66": true,
	}
	for ref, expectedResult := range tests {
		isTag := IsTagBranch(ref)
		if isTag != expectedResult {
			t.Errorf("%s: %t: got %t expected %t", ref, expectedResult, isTag, expectedResult)
		}
	}
}
