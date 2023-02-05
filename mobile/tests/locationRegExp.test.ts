import locationRegExp from "../helpers/startup/locationRegExpGen"
import * as StartupConfig from "../interfaces/startup"

const startupConfig: StartupConfig.Startups = {
    apps: [
        {
            app_name: "ru.yandex.traffic",
            startups: [
                {
                    version: ">=12.3.2",
                    productionConfig: {
                        body: "body",
                        link: "prodlink.com/12.3.2"
                    },
                    stagingConfig: {
                        body: "body",
                        link: "staginglink.com/12.3.2"
                    }
                },
                {
                    version: "2.3.4",
                    productionConfig: {
                        body: "prod body",
                        link: "prodlink.com"
                    },
                    stagingConfig: {
                        body: "staging body",
                        link: "staginglink.com"
                    }
                }
            ]
        }
    ]
}

const correctLocation = `
location ~ "^/v1/startup/ru\\.yandex\\.traffic/((12\\.3\\.(([3-9])|([1-9]\\d{1,})))|(12\\.(([4-9])|([1-9]\\d{1,}))(\\.\\d{1,}){1})|(((1[3-9])|([2-9]\\d{1})|([1-9]\\d{2,}))(\\.\\d{1,}){2})|(12\\.3\\.2))$" {
    return https://prodlink.com/12.3.2;
}
location ~ "^/v1/startup/ru\\.yandex\\.traffic/2\\.3\\.4$" {
    return https://prodlink.com;
}\n`

it("location", () => {
    expect(locationRegExp(startupConfig, true)).toEqual(correctLocation)
})