/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.core.configuration;

import com.iexec.resultproxy.api.ResultProxyClient;
import com.iexec.resultproxy.api.ResultProxyClientBuilder;
import feign.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResultRepositoryConfiguration {

    @Value("${resultRepository.protocol}")
    private String protocol;

    @Value("${resultRepository.host}")
    private String host;

    @Value("${resultRepository.port}")
    private String port;

    public String getResultRepositoryURL() {
        return protocol + "://" + host + ":" + port;
    }

    @Bean
    public ResultProxyClient resultProxyClient() {
        return ResultProxyClientBuilder.getInstance(Logger.Level.NONE, getResultRepositoryURL());
    }
}
