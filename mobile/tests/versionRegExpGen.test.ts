import versionRegExpGen from "../helpers/regexp/versionRegExpGen"

it("2.0.0", () => {
    const version = versionRegExpGen("2.0.0")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.0.0")).toBeTruthy()

    expect(regExp.test("2.0.1")).toBeFalsy()
    expect(regExp.test("2.1.0")).toBeFalsy()
    expect(regExp.test("2.1.1")).toBeFalsy()

    expect(regExp.test("3.0.0")).toBeFalsy()
    expect(regExp.test("3.0.1")).toBeFalsy()
    expect(regExp.test("3.1.1")).toBeFalsy()

    expect(regExp.test("2.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it("2.0.1", () => {
    const version = versionRegExpGen("2.0.1")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.0.1")).toBeTruthy()

    expect(regExp.test("2.0.0")).toBeFalsy()
    expect(regExp.test("2.1.0")).toBeFalsy()
    expect(regExp.test("2.1.1")).toBeFalsy()

    expect(regExp.test("3.0.0")).toBeFalsy()
    expect(regExp.test("3.0.1")).toBeFalsy()
    expect(regExp.test("3.1.1")).toBeFalsy()

    expect(regExp.test("2.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it("2.1.0", () => {
    const version = versionRegExpGen("2.1.0")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.1.0")).toBeTruthy()

    expect(regExp.test("2.0.0")).toBeFalsy()
    expect(regExp.test("2.0.1")).toBeFalsy()
    expect(regExp.test("2.1.1")).toBeFalsy()

    expect(regExp.test("3.0.0")).toBeFalsy()
    expect(regExp.test("3.0.1")).toBeFalsy()
    expect(regExp.test("3.1.1")).toBeFalsy()

    expect(regExp.test("2.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it(">=2.0.0", () => {
    const version = versionRegExpGen(">=2.0.0")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.0.0")).toBeTruthy()

    expect(regExp.test("2.0.1")).toBeTruthy()
    expect(regExp.test("2.1.0")).toBeTruthy()
    expect(regExp.test("2.1.1")).toBeTruthy()

    expect(regExp.test("3.0.0")).toBeTruthy()
    expect(regExp.test("3.0.1")).toBeTruthy()
    expect(regExp.test("3.1.1")).toBeTruthy()

    expect(regExp.test("2.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it(">=2.0.1", () => {
    const version = versionRegExpGen(">=2.0.1")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.0.1")).toBeTruthy()

    expect(regExp.test("2.1.0")).toBeTruthy()
    expect(regExp.test("2.1.1")).toBeTruthy()

    expect(regExp.test("3.0.0")).toBeTruthy()
    expect(regExp.test("3.0.1")).toBeTruthy()
    expect(regExp.test("3.1.1")).toBeTruthy()

    expect(regExp.test("2.0.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it(">=2.1.0", () => {
    const version = versionRegExpGen(">=2.1.0")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.1.0")).toBeTruthy()

    expect(regExp.test("2.1.1")).toBeTruthy()

    expect(regExp.test("3.0.0")).toBeTruthy()
    expect(regExp.test("3.0.1")).toBeTruthy()
    expect(regExp.test("3.1.1")).toBeTruthy()

    expect(regExp.test("2.0.0")).toBeFalsy()
    expect(regExp.test("2.0.1")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it(">=2.1.1", () => {
    const version = versionRegExpGen(">=2.1.1")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("2.1.1")).toBeTruthy()

    expect(regExp.test("3.0.0")).toBeTruthy()
    expect(regExp.test("3.0.1")).toBeTruthy()
    expect(regExp.test("3.1.1")).toBeTruthy()

    expect(regExp.test("2.0.0")).toBeFalsy()
    expect(regExp.test("2.0.1")).toBeFalsy()
    expect(regExp.test("2.1.0")).toBeFalsy()

    expect(regExp.test("1.0.0")).toBeFalsy()
    expect(regExp.test("1.0.1")).toBeFalsy()
    expect(regExp.test("1.1.0")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()

    expect(regExp.test("foobar")).toBeFalsy()
})

it("12.12.12", () => {
    const version = versionRegExpGen("12.12.12")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("12.12.12")).toBeTruthy()

    expect(regExp.test("12.12.120")).toBeFalsy()
    expect(regExp.test("12.120.120")).toBeFalsy()
    expect(regExp.test("120.120.120")).toBeFalsy()

    expect(regExp.test("12.12.11")).toBeFalsy()
    expect(regExp.test("12.11.12")).toBeFalsy()
    expect(regExp.test("11.12.12")).toBeFalsy()

    expect(regExp.test("11.12.120")).toBeFalsy()
    expect(regExp.test("11.120.120")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()
})

it(">=12.12.12", () => {
    const version = versionRegExpGen(">=12.12.12")
    const regExp = new RegExp(`^${version}$`)

    expect(regExp.test("12.12.12")).toBeTruthy()

    expect(regExp.test("12.12.120")).toBeTruthy()
    expect(regExp.test("12.120.120")).toBeTruthy()
    expect(regExp.test("120.120.120")).toBeTruthy()

    expect(regExp.test("12.12.11")).toBeFalsy()
    expect(regExp.test("12.11.12")).toBeFalsy()
    expect(regExp.test("11.12.12")).toBeFalsy()

    expect(regExp.test("11.12.120")).toBeFalsy()
    expect(regExp.test("11.120.120")).toBeFalsy()
    expect(regExp.test("1.1.1")).toBeFalsy()
})

it(">=2.3.4 with name", () => {
    const version = versionRegExpGen(">=2.3.4")
    const reg = `^/v1/ru\.yandex\.traffic/${version}$`
    const regExp = new RegExp(reg)
    expect(regExp.test("/v1/ru.yandex.traffic/2.3.4")).toBeTruthy()
    expect(regExp.test("/v1/ru.yandex.traffic/12.3.4")).toBeTruthy()
    expect(regExp.test("/2.3.4")).toBeFalsy()
    expect(regExp.test("2.3.4")).toBeFalsy()
    expect(regExp.test("foobar")).toBeFalsy()
})
