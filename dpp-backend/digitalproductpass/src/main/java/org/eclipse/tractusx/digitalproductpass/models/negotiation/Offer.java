/*********************************************************************************
 *
 * Tractus-X - Digital Product Passport Application
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

package org.eclipse.tractusx.digitalproductpass.models.negotiation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class consists exclusively to define attributes related to the Offer's data.
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Offer {

    /** ATTRIBUTES **/
    @JsonProperty("offerId")
    String offerId;
    @JsonProperty("assetId")
    String assetId;
    @JsonProperty("policy")
    Set policy;

    /** CONSTRUCTOR(S) **/
    public Offer(String offerId, String assetId, Set policy) {
        this.offerId = offerId;
        this.assetId = assetId;
        this.policy = policy;
    }
    public Offer() {
    }

    /** GETTERS AND SETTERS **/
    public String getOfferId() {
        return offerId;
    }
    @SuppressWarnings("Unused")
    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }
    public String getAssetId() {
        return assetId;
    }
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
    public Set getPolicy() {
        return policy;
    }
    public void setPolicy(Set policy) {
        this.policy = policy;
    }
}
