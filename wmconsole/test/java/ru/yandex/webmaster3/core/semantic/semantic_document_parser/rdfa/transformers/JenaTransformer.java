package ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.transformers;

import com.hp.hpl.jena.rdf.model.*;
import org.cyberneko.html.parsers.SAXParser;
import org.json.JSONException;
import org.semarglproject.jena.core.sink.JenaSink;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.source.StreamProcessor;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.RDFaUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.data.RDFaComplexProperty;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.data.RDFaEntity;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.data.RDFaValueProperty;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by aleksart on 20.04.15.
 */
public class JenaTransformer {


    private static final java.lang.String HTTP_EXAMPLE_COM = "http://ex.com";

    private static void benchmarkSemarglJena(InputStream in) throws SAXException, org.semarglproject.rdf.ParseException, JSONException {
        System.out.println("Semargl-Jena benchmark");
        Model model = ModelFactory.createDefaultModel();
        TripleSink tripleSink = JenaSink.connect(model);
        RdfaParser rdfaParser = (RdfaParser) RdfaParser.connect(tripleSink);
//        rdfaParser.setPropertyInternal();
        XMLReader htmlReader = new SAXParser();
//        htmlReader.setDTDHandler();
        htmlReader.setFeature("http://cyberneko.org/html/features/override-namespaces", false) ;
        htmlReader.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        htmlReader.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment",true);
        htmlReader.setFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-tags", true);
        htmlReader.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

        StreamProcessor streamProcessor = new StreamProcessor(RdfaParser.connect(JenaSink.connect(model)));
        streamProcessor.setProperty(StreamProcessor.XML_READER_PROPERTY, htmlReader);

        streamProcessor.process(in, HTTP_EXAMPLE_COM);

        System.out.println("Model size = " + model.size());

        StmtIterator iterator = model.listStatements();

        MultiMap<String,Statement> propertyHashMap = new MultiMap<String, Statement>();

        HashSet<Resource> roots = new HashSet<Resource>();

        while (iterator.hasNext()) {


            Statement stmt = iterator.nextStatement();  // get next statement

            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object

            System.out.println(subject.toString() + " " + predicate.getLocalName() + " " + object.toString());

            propertyHashMap.append(subject.toString(), stmt);
            roots.add(subject);

        }
        System.out.println("Roots");
        for(Resource root: roots){
            System.out.println(root.toString());
        }
        for(String resource : propertyHashMap.keySet()){
            for(Statement pred : propertyHashMap.get(resource)){
                try {
                    roots.remove(pred.getObject().asResource());
                } catch (ResourceRequiredException ignored){

                }
            }
        }
        System.out.println("Roots");
        for(Resource root: roots){
            System.out.println(root.toString());
        }
        List<RDFaEntity> rootEntities = new ArrayList<RDFaEntity>();
        for(Resource root: roots){
            RDFaEntity rootEntity = new RDFaEntity("",HTTP_EXAMPLE_COM);
            visitChildren(root.toString(), propertyHashMap,rootEntity);
            rootEntities.add(rootEntity);
        }
        for(RDFaEntity rootEntity: rootEntities){
            System.out.println(RDFaUtils.toTestJSON(rootEntity));
        }

    }

    private static void visitChildren(String localName, MultiMap<String, Statement> propertyHashMap, RDFaEntity rootEntity) {

        for(Statement statement: propertyHashMap.get(localName)){
            RDFNode obj = statement.getObject();
            Property predicate = statement.getPredicate();
            String pred = predicate.toString();
            if(obj.isResource()){


                RDFaEntity child = new RDFaEntity("",null);


                if(isTypeAttr(pred)){
                    rootEntity.setType(obj.toString());
                }
                if(propertyHashMap.containsKey(obj.toString())) {
                    rootEntity.appendProperty(new RDFaComplexProperty(pred,child));
                    visitChildren(obj.toString(), propertyHashMap, child);
                }
            }
            else if(obj.isURIResource()){
                rootEntity.appendProperty(new RDFaValueProperty(pred,null,obj.toString(),obj.toString()));

            }
            else if(obj.isLiteral()){
                rootEntity.appendProperty(new RDFaValueProperty(pred,obj.toString(),null,obj.toString()));

            }
        }

    }

    private static boolean isTypeAttr(String s) {
        return "http://www.w3.org/1999/02/22-rdf-syntax-ns#type".equals(s);
    }

    public static void main(String[] args) throws SAXException, IOException {


        String source = "<div vocab=\"http://schema.org/\" typeof=\"Product\">\n" +
                "  <span property=\"name\">Kenmore White 17\" Microwave</span>\n" +
                "  <img src=\"kenmore-microwave-17in.jpg\" alt='Kenmore 17\" Microwave' />\n" +
                "  <div property=\"aggregateRating\"\n" +
                "     typeof=\"AggregateRating\">\n" +
                "   Rated <span property=\"ratingValue\">3.5</span>/5\n" +
                "   based on <span property=\"reviewCount\">11</span> customer reviews\n" +
                "  </div>\n" +
                "  <div property=\"offers\"  typeof=\"Offer\">\n" +
                "    <!--price is 1000, a number, with locale-specific thousands separator\n" +
                "        and decimal mark, and the $ character is marked up with the\n" +
                "        machine-readable code \"USD\" -->\n" +
                "    <span property=\"priceCurrency\" content=\"USD\">$</span><span\n" +
                "      property=\"price\" content=\"1000.00\">1,000.00</span>\n" +
                "    <link property=\"availability\" content=\"http://schema.org/InStock\" />In stock\n" +
                "  </div>\n" +
                "  Product description:\n" +
                "  <span property=\"description\">0.7 cubic feet countertop microwave.\n" +
                "  Has six preset cooking categories and convenience features like\n" +
                "  Add-A-Minute and Child Lock.</span>\n" +
                "  Customer reviews:\n" +
                "  <div property=\"review\"  typeof=\"Review\">\n" +
                "    <span property=\"name\">Not a happy camper</span> -\n" +
                "    by <span property=\"author\">Ellie</span>,\n" +
                "    <meta property=\"datePublished\" content=\"2011-04-01\">April 1, 2011\n" +
                "    <div property=\"reviewRating\"  typeof=\"Rating\">\n" +
                "      <meta property=\"worstRating\" content = \"1\">\n" +
                "      <span property=\"ratingValue\">1</span>/\n" +
                "      <span property=\"bestRating\">5</span>stars\n" +
                "    </div>\n" +
                "    <span property=\"description\">The lamp burned out and now I have to replace\n" +
                "    it. </span>\n" +
                "  </div>\n" +
                "  <div property=\"review\"  vocab=\"https://schema.org/\" typeof=\"Review\">\n" +
                "    <span property=\"name\">Value purchase</span> -\n" +
                "    by <span property=\"author\">Lucas</span>,\n" +
                "    <meta property=\"datePublished\" content=\"2011-03-25\">March 25, 2011\n" +
                "    <div property=\"reviewRating\"  typeof=\"Rating\">\n" +
                "      <meta property=\"worstRating\" content = \"1\"/>\n" +
                "      <span property=\"ratingValue\">4</span>/\n" +
                "      <span property=\"bestRating\">5</span>stars\n" +
                "    </div>\n" +
                "    <span property=\"description\">Great microwave for the price. It is small and\n" +
                "    fits in my apartment.</span>\n" +
                "  </div>\n" +
                "  ...\n" +
                "</div>";



        InputStream in = null;
        try {
            in = new ByteArrayInputStream(source.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        OutputStream outputStream = new ByteArrayOutputStream();
//        tidy.parse(in, outputStream);
//        String out = outputStream.toString();
//        System.out.println(out);
//        in = new ByteArrayInputStream(out.getBytes("UTF-8"));



        try {
            benchmarkSemarglJena(in);
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }


    }

}
