const testDateISO = process.env.TEST_DATE ?? '';
const testDate = new Date(testDateISO);

const todayTestDate = testDate.toISOString(); // 2020-03-13T13:06:42.000Z

testDate.setDate(testDate.getDate() - 1);
const prevTestDateDay = testDate.toISOString(); // 2020-03-12T13:06:42.000Z

testDate.setDate(testDate.getDate() + 2);
const nextTestDateDay = testDate.toISOString(); // 2020-03-14T13:06:42.000Z

export {prevTestDateDay, nextTestDateDay, todayTestDate};
