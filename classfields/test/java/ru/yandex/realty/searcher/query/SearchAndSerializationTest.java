package ru.yandex.realty.searcher.query;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.extdata.core.lego.Provider;
import ru.yandex.realty.context.ExtDataRegionGraphProvider;
import ru.yandex.realty.graph.DocumentBuilderHelper;
import ru.yandex.realty.graph.RegionGraph;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.lucene.OfferDocumentDeserializer;
import ru.yandex.realty.lucene.ProtoLuceneDocumentBuilder;
import ru.yandex.realty.lucene.ProtoLuceneOfferDocumentBuilder;
import ru.yandex.realty.model.billing.Campaign;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.PriceInfo;
import ru.yandex.realty.model.sites.ExtendedSiteStatistics;
import ru.yandex.realty.picapica.MdsUrlBuilder;
import ru.yandex.realty.sites.CampaignService;
import ru.yandex.realty.sites.ExtendedSiteStatisticsStorage;
import ru.yandex.realty.sites.SitesGroupingService;
import ru.yandex.realty.sites.campaign.CampaignStorage;
import ru.yandex.realty.storage.CurrencyStorage;
import ru.yandex.realty.storage.ExpectedMetroStorage;
import ru.yandex.realty.storage.ParkStorage;
import ru.yandex.realty.storage.PondStorage;
import ru.yandex.realty.util.lucene.AllDocumentsCollectorOnList;
import ru.yandex.realty.util.lucene.DocumentBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyLong;

/**
 * User: daedra
 * Date: 18.07.14
 * Time: 16:34
 */
public abstract class SearchAndSerializationTest {
    protected CurrencyStorage currencyStorage;
    protected RegionGraph regionGraph;
    protected DocumentBuilderHelper documentBuilderHelper;
    protected DocumentBuilder<Offer> offerDocumentBuilder;
    protected Provider<RegionGraph> regionGraphProvider;

    protected Directory directory;
    protected IndexWriterConfig writerConfig;
    protected IndexWriter indexWriter;
    protected IndexReader indexReader;
    protected IndexSearcher indexSearcher;


    protected void setUp() {
        if (currencyStorage != null) {
            Mockito.reset(currencyStorage, regionGraph);
        }
        regionGraph = Mockito.mock(RegionGraph.class);
        final Node node = Mockito.mock(Node.class);
        Mockito.when(node.getGeoId()).thenReturn(47);
        Mockito.when(node.getId()).thenReturn(575243l);
        Mockito.when(regionGraph.getNodeById(Mockito.anyLong())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                long id = (Long) invocation.getArguments()[0];
                if (id > 0) return node;
                return null;
            }
        });
        Mockito.when(regionGraph.getNodeByGeoId(Mockito.anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int id = (Integer) invocation.getArguments()[0];
                if (id > 0) return node;
                return null;
            }
        });

        currencyStorage = Mockito.mock(CurrencyStorage.class);
        Mockito.when(currencyStorage.convert(Mockito.any(PriceInfo.class), Mockito.any(Currency.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });
        Mockito.when(currencyStorage.convertToCurrency(Mockito.anyFloat(), Mockito.any(Currency.class), Mockito.any(Currency.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });
        Mockito.when(currencyStorage.getDefaultCurrency(Mockito.any(Node.class))).thenReturn(Currency.RUR);

        Set<Node> nodes = new HashSet<>();
        nodes.add(node);
        Set<Integer> nodeIds = new HashSet<>();
        nodeIds.add(node.getGeoId());

        documentBuilderHelper = Mockito.mock(DocumentBuilderHelper.class);
        Mockito.when(documentBuilderHelper.getAllNodesForSearch(Mockito.any(Location.class))).thenReturn(nodes);
        Mockito.when(documentBuilderHelper.getNodesToRootForSearch(Mockito.any(Collection.class))).thenReturn(nodes);
        Mockito.when(documentBuilderHelper.getGeoIdsToRoot(Mockito.any(Node.class))).thenReturn(nodeIds);

        regionGraphProvider = Mockito.mock(ExtDataRegionGraphProvider.class);
        Mockito.when(regionGraphProvider.get()).thenReturn(regionGraph);

        SitesGroupingService sitesGroupingService = Mockito.mock(SitesGroupingService.class);
        Mockito.when(sitesGroupingService.getSiteById(anyLong())).thenReturn(null);

        Provider<ExtendedSiteStatisticsStorage> extendedSiteStatisticsStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(extendedSiteStatisticsStorageProvider.get()).thenReturn(new ExtendedSiteStatisticsStorage(Collections.<Long, ExtendedSiteStatistics>emptyMap()));

        Provider<CampaignStorage> campaignStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(campaignStorageProvider.get()).thenReturn(new CampaignStorage(Collections.<Campaign>emptyList()));

        Provider<ExpectedMetroStorage> expectedMetroStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(expectedMetroStorageProvider.get()).thenReturn(ExpectedMetroStorage.empty());

        Provider<PondStorage> pondStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(pondStorageProvider.get()).thenReturn(PondStorage.empty());

        Provider<ParkStorage> parkStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(parkStorageProvider.get()).thenReturn(ParkStorage.empty());

        ProtoLuceneDocumentBuilder protoDocumentBuilder = new ProtoLuceneDocumentBuilder(regionGraphProvider,
                documentBuilderHelper,
                sitesGroupingService,
                extendedSiteStatisticsStorageProvider,
                new CampaignService(campaignStorageProvider),
                pondStorageProvider,
                parkStorageProvider,
                new MdsUrlBuilder("//avatarnica.test")
        );
        offerDocumentBuilder = new ProtoLuceneOfferDocumentBuilder(protoDocumentBuilder);
    }

    protected void serialize(Offer... offers) throws IOException {
        directory = new RAMDirectory();
        writerConfig = new IndexWriterConfig(new KeywordAnalyzer());
        indexWriter = new IndexWriter(directory, writerConfig);

        for (Offer offer : offers) {
            Document document = offerDocumentBuilder.serialize(offer);
            indexWriter.addDocument(document);
        }
        indexWriter.close();

        indexReader = DirectoryReader.open(directory);
        indexSearcher = new IndexSearcher(indexReader);
    }

    protected List<Offer> search(Query query) throws IOException {
        AllDocumentsCollectorOnList allDocumentsCollector = new AllDocumentsCollectorOnList();
        indexSearcher.search(query, allDocumentsCollector);
        List<Offer> result = new LinkedList<>();
        for (Integer docId : allDocumentsCollector.getDocs()) {
            result.add(OfferDocumentDeserializer.toOffer(indexReader.document(docId)));
        }
        return result;
    }

    protected void cleanup() throws IOException {
        indexReader.close();
        directory.close();
    }
}
