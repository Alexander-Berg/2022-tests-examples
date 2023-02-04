package provider

import (
	"fmt"
	"github.com/hashicorp/terraform-plugin-sdk/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/terraform"
	"net/http"
	"testing"
	"time"
)

func TestAccConductorHostService(t *testing.T) {
	resource.Test(t, resource.TestCase{
		Providers:    testAccProviders,
		CheckDestroy: testAccCheckConductorHostServiceDestroy,
		Steps: []resource.TestStep{
			{
				Config: testCreateServiceConfig,
				Check: resource.ComposeTestCheckFunc(
					testAccCheckConductorHostServiceExists("conductor_host.manage_host"),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "short_name", shortName["sas"]),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "fqdn", fqdn["sas"]),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "dc", "sas"),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "group", "vertis_parking"),
				),
			},
			{
				Config: testUpdateServiceConfig,
				Check: resource.ComposeTestCheckFunc(
					testAccCheckConductorHostServiceExists("conductor_host.manage_host"),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "short_name", shortName["myt"]),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "fqdn", fqdn["myt"]),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "dc", "myt"),
					resource.TestCheckResourceAttr("conductor_host.manage_host", "group", "vertis_parking"),
				),
			},
		},
	})
}

func testAccCheckConductorHostServiceExists(resourceKey string) resource.TestCheckFunc {
	return func(state *terraform.State) error {

		dc := getDc()

		rs, ok := state.RootModule().Resources[resourceKey]
		if !ok {
			return fmt.Errorf("not found: %s", resourceKey)
		}

		if rs.Primary.ID != fqdn[dc] {
			return fmt.Errorf("no ID is set")
		}
		endpoint := testAccProvider.Meta().(*Config).Endpoint
		token := testAccProvider.Meta().(*Config).Token
		url := fmt.Sprintf("%s/api/get_host_extensions/%s", endpoint, fqdn[dc])

		httpStatusCode, err := checkHttpResponceCode(httpClient, url, token)
		if err != nil {
			return err
		}
		if httpStatusCode != http.StatusOK {
			return fmt.Errorf("host %s not created in conductor", fqdn[dc])
		}

		fmt.Println(fqdn[dc], " created")
		testStep = "update"
		return nil
	}
}

func testAccCheckConductorHostServiceDestroy(state *terraform.State) error {

	dc := getDc()
	resources := getResourcesByType("conductor_host", state)
	if len(resources) != 1 {
		return fmt.Errorf("expecting only 1 service resource found %v", len(resources))
	}
	if resources[0].Type != "conductor_host" {
		return fmt.Errorf("resourse type \"conductor_host\" not found")
	}

	endpoint := testAccProvider.Meta().(*Config).Endpoint
	token := testAccProvider.Meta().(*Config).Token
	url := fmt.Sprintf("%s/api/get_host_extensions/%s", endpoint, fqdn[dc])

	for i := range fqdn {
		httpStatusCode, err := checkHttpResponceCode(httpClient, url, token)
		if err != nil {
			return err
		}
		if httpStatusCode != http.StatusNotFound {
			return fmt.Errorf("host %s still exists in conductor", fqdn[i])
		}
		fmt.Println(fqdn[i], " not found, OK")
	}
	return nil
}

func getResourcesByType(resourceType string, state *terraform.State) []*terraform.ResourceState {

	var result []*terraform.ResourceState
	for _, rs := range state.RootModule().Resources {
		if rs.Type == resourceType {
			result = append(result, rs)
		}
	}

	return result
}

func checkHttpResponceCode(client http.Client, url, token string) (int, error) {

	// do not run so fast, it is for conductor
	time.Sleep(2 * time.Second)

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return -1, err
	}
	req.Header.Add("Authorization", "OAuth "+token)
	resp, err := httpClient.Do(req)
	if err != nil {
		return -1, err
	}
	defer resp.Body.Close()
	return resp.StatusCode, nil
}

func getDc() string {
	if testStep == "create" {
		return "sas"
	}
	return "myt"
}

const testCreateServiceConfig = `
resource "conductor_host" "manage_host" {
  count = 1
  short_name = "delete-me-terraform-test-01-sas.test.vertis"
  fqdn       = "delete-me-terraform-test-01-sas.test.vertis.yandex.net"
  dc         = "sas"
  group      = "vertis_parking"
}
`
const testUpdateServiceConfig = `
resource "conductor_host" "manage_host" {
  count = 1
  short_name = "delete-me-terraform-test-01-myt.test.vertis"
  fqdn       = "delete-me-terraform-test-01-myt.test.vertis.yandex.net"
  dc         = "myt"
  group      = "vertis_parking"
}
`

var (
	shortName = map[string]string{
		"sas": "delete-me-terraform-test-01-sas.test.vertis",
		"myt": "delete-me-terraform-test-01-myt.test.vertis",
	}
	fqdn = map[string]string{
		"sas": "delete-me-terraform-test-01-sas.test.vertis.yandex.net",
		"myt": "delete-me-terraform-test-01-myt.test.vertis.yandex.net",
	}

	testStep = "create"
)
