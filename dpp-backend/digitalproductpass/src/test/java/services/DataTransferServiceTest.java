/*********************************************************************************
 *
 * Tractus-X - Digital Product Pass Application
 *
 * Copyright (c) 2022, 2024 BMW AG, Henkel AG & Co. KGaA
 * Copyright (c) 2023, 2024 CGI Deutschland B.V. & Co. KG
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 *
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the
 * License for the specific language govern in permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package services;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.tractusx.digitalproductpass.config.DtrConfig;
import org.eclipse.tractusx.digitalproductpass.config.ProcessConfig;
import org.eclipse.tractusx.digitalproductpass.config.VaultConfig;
import org.eclipse.tractusx.digitalproductpass.exceptions.ServiceInitializationException;
import org.eclipse.tractusx.digitalproductpass.managers.ProcessManager;
import org.eclipse.tractusx.digitalproductpass.models.http.responses.IdResponse;
import org.eclipse.tractusx.digitalproductpass.models.manager.Status;
import org.eclipse.tractusx.digitalproductpass.models.negotiation.*;
import org.eclipse.tractusx.digitalproductpass.services.DataTransferService;
import org.eclipse.tractusx.digitalproductpass.services.VaultService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonarsource.scanner.api.internal.shaded.minimaljson.Json;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import utils.*;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataTransferServiceTest {

    private DataTransferService dataTransferService;
    private Dataset dataSet;
    private Set policy;
    private String bpn;
    private final String testPolicyPath = "/dpp/contractpolicies/TestPolicy.json";
    private final String testDTCatalogPath = "/dpp/catalogs/TestDigitalTwinCatalog.json";
    private final String testCOCatalogPath = "/dpp/catalogs/TestContractOfferCatalog.json";
    private final String testResponseInitNegotiationPath = "/dpp/negotiation/TestResponseInitNegotiation.json";
    private final String testResponseNegotiationPath = "/dpp/negotiation/TestResponseNegotiation.json";
    private final String testResponseInitTransferPath = "/dpp/transfer/TestResponseInitTransfer.json";
    private final String testResponseTransferPath = "/dpp/transfer/TestResponseTransfer.json";
    @Mock
    private VaultService vaultService;
    @Mock
    private ProcessManager processManager;
    @Mock
    private DtrConfig dtrConfig;
    @Mock
    private Environment env;
    @Mock
    private HttpUtil httpUtil;

    private EdcUtil edcUtil;
    private JsonUtil jsonUtil;
    private YamlUtil yamlUtil;
    private FileUtil fileUtil;

    @BeforeAll
    void setUpAll() throws ServiceInitializationException {
        MockitoAnnotations.openMocks(this);
        dtrConfig = initDtrConfig();
        fileUtil = new FileUtil();
        jsonUtil = new JsonUtil(fileUtil);
        edcUtil = new EdcUtil(jsonUtil);
        yamlUtil = new YamlUtil(fileUtil);
        env = initEnv();
        bpn = "BPNL00000000000";
        String mockApiKey = "12345678979ayasdmasdjncjxnzc";
        when(vaultService.getLocalSecret("edc.apiKey")).thenReturn(mockApiKey);
        when(vaultService.getLocalSecret("edc.participantId")).thenReturn(bpn);


        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setDir("process");
        processManager = new ProcessManager(httpUtil, jsonUtil, fileUtil, processConfig);

        dataTransferService = new DataTransferService(env, httpUtil,edcUtil, jsonUtil, vaultService, processManager, dtrConfig);

        when(httpUtil.getHeaders()).thenReturn(new HttpHeaders());
        when(httpUtil.getParams()).thenReturn(new HashMap<>());
    }

    @BeforeEach
    void setUp() {
        String filePath = Paths.get(fileUtil.getBaseClassDir(this.getClass()), testPolicyPath).toString();
        Set policy = (Set) jsonUtil.fromJsonFileToObject(filePath, Set.class);

        Dataset dataSet = new Dataset();
        String dataSetId = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();
        dataSet.setId(dataSetId);
        dataSet.setType("test-type");
        dataSet.setAssetId(assetId);
        dataSet.setPolicy(List.of(policy));

        this.dataSet = dataSet;
        this.policy = policy;
    }

    private MockEnvironment initEnv() {
        MockEnvironment env = new MockEnvironment();
        String configurationFilePath = Paths.get(fileUtil.getBaseClassDir(this.getClass()), "application-test.yml").toString();
        Map<String, Object> application = yamlUtil.readFile(configurationFilePath);
        Map<String, Object> configuration = (Map<String, Object>) jsonUtil.toMap(application.get("configuration"));
        Map<String, Object> edc = (Map<String, Object>) jsonUtil.toMap(configuration.get("edc"));
        env.setProperty("configuration.edc.endpoint", edc.get("endpoint").toString());
        env.setProperty("configuration.edc.catalog", edc.get("catalog").toString());
        env.setProperty("configuration.edc.management", edc.get("management").toString());
        env.setProperty("configuration.edc.negotiation", edc.get("negotiation").toString());
        env.setProperty("configuration.edc.transfer", edc.get("transfer").toString());

        return env;
    }

    private DtrConfig initDtrConfig() {
        DtrConfig dtrConfig = new DtrConfig();
        DtrConfig.Timeouts timeouts = new DtrConfig.Timeouts();
        timeouts.setSearch(10);
        timeouts.setDtrRequestProcess(40);
        timeouts.setNegotiation(10);
        timeouts.setTransfer(10);
        timeouts.setDigitalTwin(20);
        dtrConfig.setTimeouts(timeouts);
        dtrConfig.setTemporaryStorage(new DtrConfig.TemporaryStorage(true, 12));
        return dtrConfig;
    }

    private VaultConfig initVaultConfig() {
        String configurationFilePath = Paths.get(fileUtil.getBaseClassDir(this.getClass()), "application-test.yml").toString();
        Map<String, Object> application = yamlUtil.readFile(configurationFilePath);
        Map<String, Object> configuration = (Map<String, Object>) jsonUtil.toMap(application.get("configuration"));
        Map<String, Object> vault = (Map<String, Object>) jsonUtil.toMap(configuration.get("vault"));
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setFile(vault.get("file").toString());

        return vaultConfig;
    }

    @Test
    void checkEdcConsumerConnection() {
        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(Object.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testCOCatalogPath).toString(), JsonNode.class), HttpStatus.OK));

        String participantId = dataTransferService.getEdcConnectorBpn();

        assertNotNull(participantId);
        assertEquals(vaultService.getLocalSecret("edc.participantId"), participantId);
    }

    @Test
    void buildRequestAndOffer() {
        Status status = new Status();
        status.setEndpoint("test.endpoint");

        NegotiationRequest negotiationRequest = dataTransferService.buildRequest(dataSet, status, bpn, bpn);

        assertNotNull(negotiationRequest);
        assertEquals(status.getEndpoint(), negotiationRequest.getConnectorAddress());
        assertEquals(bpn, negotiationRequest.getConnectorId());

        Offer offer = negotiationRequest.getOffer();

        assertNotNull(offer);
        assertEquals(dataSet.getAssetId(), offer.getAssetId());
        assertEquals(policy.getId(), offer.getOfferId());
        assertNotNull(offer.getPolicy());
    }

    @Test
    void getContractOfferCatalog() {
        String providerUrl = UUID.randomUUID().toString();
        Catalog catalog = (Catalog) jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testCOCatalogPath).toString(), Catalog.class);
        String assetId = catalog.getParticipantId();

        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(Object.class), eq(false), eq(false)))
                .then(invocation -> {
                    CatalogRequest body = invocation.getArgument(4);
                    if (body.getCounterPartyAddress().equals(providerUrl)) {
                        return new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testCOCatalogPath).toString(), JsonNode.class), HttpStatus.OK);
                    }
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                });

        Catalog offerCatalog = dataTransferService.getContractOfferCatalog(providerUrl, assetId);

        assertNotNull(offerCatalog);
        assertNotEquals(catalog, offerCatalog);
        assertEquals(catalog.getId(), offerCatalog.getId());
        assertEquals(catalog.getType(), offerCatalog.getType());
        assertEquals(catalog.getParticipantId(), offerCatalog.getParticipantId());
        assertEquals(catalog.getContext(), offerCatalog.getContext());

        Map<String, String> contractOffer = (Map<String, String>) jsonUtil.toMap(offerCatalog.getContractOffers());
        assertEquals("batterypass test data", contractOffer.get("description"));

    }

    @Test
    void searchDigitalTwinCatalog() {
        String providerUrl = UUID.randomUUID().toString();
        Catalog catalog = (Catalog) jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testDTCatalogPath).toString(), Catalog.class);

        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(Object.class), eq(false), eq(false)))
                .then(invocation -> {
                    CatalogRequest body = invocation.getArgument(4);
                    if (body.getCounterPartyAddress().equals(CatenaXUtil.buildDataEndpoint(providerUrl))) {
                        return new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testDTCatalogPath).toString(), JsonNode.class), HttpStatus.OK);
                    }
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                });

        Catalog digitalTwinCatalog = dataTransferService.searchDigitalTwinCatalog(providerUrl);

        assertNotNull(digitalTwinCatalog);
        assertNotEquals(catalog, digitalTwinCatalog);
        assertEquals(catalog.getId(), digitalTwinCatalog.getId());
        assertEquals(catalog.getType(), digitalTwinCatalog.getType());
        assertEquals(catalog.getParticipantId(), digitalTwinCatalog.getParticipantId());
        assertEquals(catalog.getContext(), digitalTwinCatalog.getContext());

        Map<String, String> contractOffer = (Map<String, String>) jsonUtil.toMap(digitalTwinCatalog.getContractOffers());
        assertEquals("data.core.digitalTwinRegistry", contractOffer.get("type"));
    }

    @Test
    void doContractNegotiationAndSeeNegotiation() {
        String providerUrl = UUID.randomUUID().toString();

        Offer offer = dataTransferService.buildOffer(dataSet, 0);

        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(NegotiationRequest.class), eq(false), eq(false)))
                .then(invocation -> {
                    NegotiationRequest body = invocation.getArgument(4);
                    if (body instanceof NegotiationRequest) {
                        return new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseInitNegotiationPath).toString(), JsonNode.class), HttpStatus.OK);
                    }
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                });

        IdResponse response = dataTransferService.doContractNegotiation(offer, bpn, bpn, providerUrl);

        assertNotNull(response);
        assertEquals("189f4957-0fbe-4d73-b215-977e3303a45e", response.getId());
        assertEquals("IdResponseDto", response.getType());

        when(httpUtil.doGet(anyString(), eq(NegotiationTransferResponse.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseNegotiationPath).toString(), Negotiation.class), HttpStatus.OK));

        when(httpUtil.doGet(anyString(), eq(JsonNode.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseNegotiationPath).toString(), JsonNode.class), HttpStatus.OK));

        Negotiation negotiation = dataTransferService.seeNegotiation(response.getId());

        assertEquals(response.getId(), negotiation.getId());
        assertEquals("ContractNegotiationDto", negotiation.getType());
        assertEquals("FINALIZED", negotiation.getState());
    }

    private Negotiation getNegotiation() {
        String providerUrl = UUID.randomUUID().toString();
        Offer offer = dataTransferService.buildOffer(dataSet, 0);

        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(NegotiationRequest.class), eq(false), eq(false)))
                .then(invocation -> {
                    NegotiationRequest body = invocation.getArgument(4);
                    if (body instanceof NegotiationRequest) {
                        return new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseInitNegotiationPath).toString(), JsonNode.class), HttpStatus.OK);
                    }
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                });

        IdResponse negotiationResponse = dataTransferService.doContractNegotiation(offer, bpn,bpn,  providerUrl);

        when(httpUtil.doGet(anyString(), eq(NegotiationTransferResponse.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseNegotiationPath).toString(), Negotiation.class), HttpStatus.OK));

        when(httpUtil.doGet(anyString(), eq(JsonNode.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseNegotiationPath).toString(), JsonNode.class), HttpStatus.OK));

        return dataTransferService.seeNegotiation(negotiationResponse.getId());
    }

    @Test
    void initiateTransferAndSeeTransfer() {

        Negotiation negotiation = getNegotiation();
        Status status = new Status();
        status.setEndpoint("test.endpoint");

        TransferRequest transferRequest = new TransferRequest(
                jsonUtil.toJsonNode(Map.of("odrl", "http://www.w3.org/ns/odrl/2/")),
                dataSet.getAssetId(),
                status.getEndpoint(),
                bpn,
                negotiation.getContractAgreementId(),
                null,
                false,
                null,
                "dataspace-protocol-http",
                null
        );

        when(httpUtil.doPost(anyString(), any(Class.class), any(HttpHeaders.class), any(Map.class), any(Object.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseInitTransferPath).toString(), JsonNode.class).toString(), HttpStatus.OK));

        IdResponse response = dataTransferService.initiateTransfer(transferRequest);

        assertNotNull(response);
        assertEquals("9ab72e5b-f2d4-4f60-85e6-0985f9b6b579", response.getId());
        assertEquals("IdResponseDto", response.getType());

        when(httpUtil.doGet(anyString(), eq(NegotiationTransferResponse.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseTransferPath).toString(), Transfer.class), HttpStatus.OK));

        when(httpUtil.doGet(anyString(), eq(JsonNode.class), any(HttpHeaders.class), any(Map.class), eq(false), eq(false)))
                .thenReturn(new ResponseEntity<>(jsonUtil.fromJsonFileToObject(Paths.get(fileUtil.getBaseClassDir(this.getClass()), testResponseTransferPath).toString(), JsonNode.class), HttpStatus.OK));

        Transfer transfer = dataTransferService.seeTransfer(response.getId());

        assertEquals(response.getId(), transfer.getId());
        assertEquals("TransferProcessDto", transfer.getType());
        assertEquals("COMPLETED", transfer.getState());
    }
}
