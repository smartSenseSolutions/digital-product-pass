/**********************************************************
 *
 * Catena-X - Material Passport Consumer Backend
 *
 * Copyright (c) 2022: CGI Deutschland B.V. & Co. KG
 * Copyright (c) 2022 Contributors to the CatenaX (ng) GitHub Organisation.
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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************/

package tools;

import tools.exceptions.ToolException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class configTools {

    private static final String CONFIGURATION_DIR = "config";

    private static final String CONFIGURATION_FILE_NAME = "configuration";
    private static final String CONFIGURATION_FILE_PATH = CONFIGURATION_DIR+"/"+CONFIGURATION_FILE_NAME+".yml";
    private Map<String, Object> configuration;
    private static final List<String> AVAILABLE_ENVIRONMENTS = List.of("dev", "int");

    public Map<String, Object> mainConfig;
    public String environment;

    private final String ENV_CONFIGURATION_FILE_PATH;

    public configTools(){
        InputStream mainConfigContent  = fileTools.getResourceContent(this.getClass(), CONFIGURATION_FILE_PATH);
        this.mainConfig = yamlTools.parseYmlStream(mainConfigContent);

        if(!this.mainConfig.containsKey("environment")){
            throw new ToolException(configTools.class,"[CRITICAL] Configuration file ["+ CONFIGURATION_FILE_PATH +"] not contains environment key");
        }

        this.environment = (String) this.mainConfig.get("environment");
        if(this.environment == null || this.environment.isEmpty() || !AVAILABLE_ENVIRONMENTS.contains(this.environment)){
            throw new ToolException(configTools.class,"[CRITICAL] Configuration file ["+ CONFIGURATION_FILE_PATH +"] not a correct environment");
        }

        this.ENV_CONFIGURATION_FILE_PATH = CONFIGURATION_DIR+"/"+CONFIGURATION_FILE_NAME+"-"+this.environment+".yml";
        InputStream fileContent  = fileTools.getResourceContent(this.getClass(), this.ENV_CONFIGURATION_FILE_PATH);
        this.configuration = yamlTools.parseYmlStream(fileContent);
    }
    public Map<String, Object> getConfiguration(){
        if (this.configuration == null) {
            throw new ToolException(configTools.class,"[CRITICAL] Configuration file ["+ this.ENV_CONFIGURATION_FILE_PATH +"] not loaded!");
        }
        return this.configuration;
    }
    public void loadConfiguration(String configurationFile){
        InputStream configurationContent = fileTools.getResourceContent(this.getClass(), configurationFile);
        this.configuration = yamlTools.parseYmlStream(configurationContent);
    }
    public Object getConfigurationParam(String param){
        if(this.configuration == null){
            return null;
        }
        Object value = configuration.get(param);
        if (value == null) {
            throw new ToolException(configTools.class,"[ERROR] Configuration param ["+param+"] not found!");
        }
        return value;
    }
    public Object getConfigurationParam(String param, String separator, Object defaultValue){
        if(this.configuration == null){
            return defaultValue;
        }
        Object value = jsonTools.getValue(this.configuration, param, separator, defaultValue);
        if (value == defaultValue) {
            throw new ToolException(configTools.class,"[ERROR] Configuration param ["+param+"] not found!");
        }
        return value;
    }
}
