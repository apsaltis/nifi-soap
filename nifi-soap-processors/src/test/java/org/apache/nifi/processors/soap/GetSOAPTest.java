/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.soap;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.common.OMNamespaceImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.*;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.Cookie;
import org.mockserver.model.Delay;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;
import static org.mockito.Mockito.*;

public class GetSOAPTest {

    private TestRunner testRunner;
//    @Rule
//    public MockServerRule mockServerRule = new MockServerRule(1080,this);

    private static ClientAndServer mockServer;
   // private MockServerClient mockServerClient;

    private static String wsdl = "<?xml version=\"1.0\"?>\n" +
            "<definitions name=\"TestService\"\n" +
            "             targetNamespace=\"http://localhost.com/stockquote.wsdl\"\n" +
            "             xmlns:tns=\"http://localhost.com/stockquote.wsdl\"\n" +
            "             xmlns:xsd1=\"http://localhost.com/stockquote.xsd\"\n" +
            "             xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
            "             xmlns=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "\n" +
            "  <types>\n" +
            "    <schema targetNamespace=\"http://localhost.com/stockquote.xsd\"\n" +
            "            xmlns=\"http://www.w3.org/2000/10/XMLSchema\">\n" +
            "      <element name=\"TradePriceRequest\">\n" +
            "        <complexType>\n" +
            "          <all>\n" +
            "            <element name=\"tickerSymbol\" type=\"string\"/>\n" +
            "          </all>\n" +
            "        </complexType>\n" +
            "      </element>\n" +
            "      <element name=\"TradePrice\">\n" +
            "         <complexType>\n" +
            "           <all>\n" +
            "             <element name=\"price\" type=\"float\"/>\n" +
            "           </all>\n" +
            "         </complexType>\n" +
            "      </element>\n" +
            "    </schema>\n" +
            "  </types>\n" +
            "\n" +
            "  <message name=\"GetLastTradePriceInput\">\n" +
            "    <part name=\"body\" element=\"xsd1:TradePriceRequest\"/>\n" +
            "  </message>\n" +
            "\n" +
            "  <message name=\"GetLastTradePriceOutput\">\n" +
            "    <part name=\"body\" element=\"xsd1:TradePrice\"/>\n" +
            "  </message>\n" +
            "\n" +
            "  <portType name=\"StockQuotePortType\">\n" +
            "    <operation name=\"GetLastTradePrice\">\n" +
            "      <input message=\"tns:GetLastTradePriceInput\"/>\n" +
            "      <output message=\"tns:GetLastTradePriceOutput\"/>\n" +
            "    </operation>\n" +
            "  </portType>\n" +
            "\n" +
            "  <binding name=\"StockQuoteSoapBinding\" type=\"tns:StockQuotePortType\">\n" +
            "    <soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "    <operation name=\"GetLastTradePrice\">\n" +
            "      <soap:operation soapAction=\"http://localhost.com/GetLastTradePrice\"/>\n" +
            "      <input>\n" +
            "        <soap:body use=\"literal\"/>\n" +
            "      </input>\n" +
            "      <output>\n" +
            "        <soap:body use=\"literal\"/>\n" +
            "      </output>\n" +
            "    </operation>\n" +
            "  </binding>\n" +
            "\n" +
            "  <service name=\"StockQuoteService\">\n" +
            "    <documentation>My first service</documentation>\n" +
            "    <port name=\"StockQuotePort\" binding=\"tns:StockQuoteSoapBinding\">\n" +
            "      <soap:address location=\"http://localhost.com/stockquote\"/>\n" +
            "    </port>\n" +
            "  </service>\n" +
            "\n" +
            "</definitions>";

    @BeforeClass
    public static void setup() {
        mockServer = startClientAndServer(1080);

       // mockServerClient = new MockServerClient("127.0.0.1", 1080);

    }
    @AfterClass
    public static void tearDown(){
        mockServer.stop();
    }

    @Before
    public  void init() {
        testRunner = TestRunners.newTestRunner(GetSOAP.class);

        // mockServerClient = new MockServerClient("127.0.0.1", 1080);
    }
    @After
    public void after(){
        testRunner.shutdown();
    }

    @Test
    public void testHTTPUsernamePasswordProcessor() throws IOException {



        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        new MockServerClient("127.0.0.1", 1080).when(request()
                        .withMethod("POST")
        )
                .respond(
                        response()
                                .withBody(xmlBody)
                );

        testRunner.setProperty(GetSOAP.ENDPOINT_URL,"http://localhost:1080/test_path");
        testRunner.setProperty(GetSOAP.WSDL_URL,"http://localhost:1080/test_path.wsdl");
        testRunner.setProperty(GetSOAP.METHOD_NAME,"testMethod");


        testRunner.run();

        final Relationship REL_SUCCESS = new Relationship.Builder()
                .name("success")
                .description("All FlowFiles that are created are routed to this relationship")
                .build();
        testRunner.assertAllFlowFilesTransferred(REL_SUCCESS,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<?xml version='1.0'?><dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'><latLonList>35.9153,-79.0838</latLonList></dwml>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());


    }

    @Test
    public void testHTTPWithUsernamePasswordProcessor() throws IOException {


        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        new MockServerClient("127.0.0.1", 1080).when(request()
                .withMethod("POST")
        )
                .respond(
                        response()
                                .withBody(xmlBody)
                );

        testRunner.setProperty(GetSOAP.ENDPOINT_URL,"http://localhost:1080/test_path");
        testRunner.setProperty(GetSOAP.WSDL_URL,"http://localhost:1080/test_path.wsdl");
        testRunner.setProperty(GetSOAP.METHOD_NAME,"testMethod");
        testRunner.setProperty(GetSOAP.USER_NAME,"username");
        testRunner.setProperty(GetSOAP.PASSWORD,"password");


        testRunner.run();

        final Relationship REL_SUCCESS = new Relationship.Builder()
                .name("success")
                .description("All FlowFiles that are created are routed to this relationship")
                .build();
        testRunner.assertAllFlowFilesTransferred(REL_SUCCESS,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<?xml version='1.0'?><dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'><latLonList>35.9153,-79.0838</latLonList></dwml>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());


    }

    @Test
    @Ignore
    public void testGeoServiceHTTPWithArgumentsProcessor() throws IOException {


        testRunner.setProperty(GetSOAP.ENDPOINT_URL,"http://graphical.weather.gov/xml/SOAP_server/ndfdXMLserver.php");
        testRunner.setProperty(GetSOAP.WSDL_URL,"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl");
        testRunner.setProperty(GetSOAP.METHOD_NAME,"LatLonListZipCode");
        testRunner.setProperty("zipCodeList","27510");
        testRunner.run();

        final Relationship REL_SUCCESS = new Relationship.Builder()
                .name("success")
                .description("All FlowFiles that are created are routed to this relationship")
                .build();
        testRunner.assertAllFlowFilesTransferred(REL_SUCCESS,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<?xml version='1.0'?><dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'><latLonList>35.9153,-79.0838</latLonList></dwml>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());


    }

    @Test
    public void testRelationships(){
        GetSOAP getSOAP = new GetSOAP();
        Set<Relationship> relationshipSet = getSOAP.getRelationships();
        assert(null != relationshipSet);
        assert(1 == relationshipSet.size());

        final Relationship REL_SUCCESS = new Relationship.Builder()
                .name("success")
                .description("All FlowFiles that are created are routed to this relationship")
                .build();

        assert(0 == relationshipSet.iterator().next().compareTo(REL_SUCCESS));
    }
    @Test
    public void testGetSoapMethod(){

        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNamespace = fac.createOMNamespace(namespaceUrl,namespacePrefix);

        GetSOAP getSOAP = new GetSOAP();
        OMElement element = getSOAP.getSoapMethod(fac,omNamespace,"testMethod");
        assert(null != element);
        assert(namespaceUrl.contentEquals(element.getNamespaceURI()));
        assert(localName.contentEquals(element.getLocalName()));

    }

    @Test
    public void testAddArguments(){
        //addArgumentsToMethod(ProcessContext context, OMFactory fac, OMNamespace omNamespace, OMElement method)
        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNamespace = fac.createOMNamespace(namespaceUrl,namespacePrefix);
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);


        PropertyDescriptor arg1 = new PropertyDescriptor
                .Builder()
                .name("Argument1")
                .defaultValue("60000")
                .description("The timeout value to use waiting to establish a connection to the web service")
                .dynamic(true)
                .expressionLanguageSupported(false)
                .build();

        testRunner.setProperty(arg1,"111");

        GetSOAP getSOAP = new GetSOAP();
        getSOAP.addArgumentsToMethod(testRunner.getProcessContext(),fac,omNamespace,expectedElement);
        Iterator<OMElement> childItr = expectedElement.getChildElements();
        assert(null != childItr);
        assert(childItr.hasNext());
        assert(arg1.getName().contentEquals(childItr.next().getLocalName()));
        assert(!childItr.hasNext());

    }

    @Test
    public void testProcessResult() throws IOException {
        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);
        OMElementImpl childElement = new OMElementImpl();
        childElement.setText(xmlBody);
        expectedElement.addChild(childElement);

        GetSOAP getSOAP = new GetSOAP();
        FlowFile flowFile = getSOAP.processSoapRequest(testRunner.getProcessSessionFactory().createSession(),expectedElement);
        assert(null != flowFile);
        ((MockFlowFile)flowFile).assertContentEquals(xmlBody.getBytes());

    }

}
