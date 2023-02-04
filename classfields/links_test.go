package links

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"regexp"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
)

func findAnchor(url string) (string, error) {
	r := regexp.MustCompile(`^[^\#]*\#([^/]*)$`)
	anchor := r.FindStringSubmatch(url)
	if len(anchor) == 0 {
		return "", nil
	}
	return anchor[1], nil
}

func crawLink(url string, token string) error {
	req, err := http.NewRequest(http.MethodGet, url, nil)
	req.Header.Set("authorization", fmt.Sprintf("OAUTH %s", token))
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	if err != nil {
		return err
	}
	if resp.StatusCode == http.StatusNotFound {
		return fmt.Errorf("not 200 status code %d on %s", resp.StatusCode, url)
	}
	anchor, err := findAnchor(url)
	if err != nil {
		return err
	}
	if anchor != "" {
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return err
		}
		if !strings.Contains(string(body), fmt.Sprintf("id=\"%s\"", anchor)) {
			return fmt.Errorf("cant find anchor %s on %s", anchor, url)
		}
		return nil

	}
	return nil
}

func TestLinks(t *testing.T) {
	token := os.Getenv("DOCS_OAUTH")
	links := []string{ServiceMap,
		ServiceMapType,
		ServiceMapDependsOn,
		ServiceMapStarTrack,
		ServiceMapContainerName,
		ServiceMapInterfaceName,
		ServiceMapOwners,
		ServiceMapMdbCluster,
		ServiceMapPort,
		ServiceMapService,
		ServiceMapProvidersDescription,
		ServiceMapProvidersProtocol,
		ServiceMapName,
		ServiceMapSrc,
		ServiceMapDescription,
		ServiceMapBatch,
		ServiceMapLanguage,
		ServiceRequirements,
		ServicePreparationDefaultEnv,
		ServiceRequirementsMonitoring,
		ServiceRequirementsHealthCheck,
		TelegramBot,
		DeployManifest,
		Branch,
		Templates,
		PeriodicSpec,
		Sox,
		DeployManifestPeriodic,
		DeployManifestDataCenter,
		DeployManifestMemory,
		DeployManifestResources,
		DeployManifestGeoBase,
		DeployManifestAppConf,
		DeployManifestCommonParams,
		DeployManifestLayer,
		DeployManifestConf,
		DeployManifestVars,
		DeployManifestName,
		PeriodicSpecFeatures,
		BranchName,
		TemplatesSecrets,
		TemplatesCLI,
		SoxFeatures,
		TelegramBotSub,
		Autogeneration,
	}
	for _, link := range links {
		err := crawLink(link, token)
		require.NoError(t, err)
	}
}
