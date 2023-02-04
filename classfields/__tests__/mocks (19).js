function getDocument(year) {
    return {
        url: '#' + year,
        name: `Финансовая отчетность за ${year} год`,
        fileSize: '1',
        fileExtension: 'pdf'
    };
}

const commonDeveloperData = {
    id: '1',
    name: 'Самолет',
    isExtended: true
};

const commonFinancePerformance = {
    fiscalYear: 2019,
    revenue: '33717000000',
    profitLoss: '-274740000000',
    inputVolume: 118607,
    totalVolume: 3123,
    percentageVolume: 63
};

export const developerWithoutDocuments = {
    ...commonDeveloperData,
    financialPerformance: [ {
        ...commonFinancePerformance,
        documents: []
    } ]
};

export const developerWithOneDocument = {
    ...commonDeveloperData,
    financialPerformance: [
        {
            ...commonFinancePerformance,
            profitLoss: undefined,
            inputVolume: undefined,
            documents: [ getDocument(2019) ]
        }
    ]
};

export const developerWithTwoDocument = {
    ...commonDeveloperData,
    financialPerformance: [
        {
            ...commonFinancePerformance,
            documents: [ getDocument(2019), getDocument(2017) ]
        }
    ]
};

export const developerWithFiveDocument = {
    ...commonDeveloperData,
    financialPerformance: [
        {
            fiscalYear: 2019,
            documents: [ getDocument(2019), getDocument(2018), getDocument(2017), getDocument(2016), getDocument(2015) ]
        }
    ]
};

