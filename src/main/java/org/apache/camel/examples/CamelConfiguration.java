/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.examples;

import javax.sql.DataSource;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.spi.ComponentCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class CamelConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

  @Bean
  ComponentCustomizer<SqlComponent> sqlComponentCustomizer(DataSource dataSource) {
    return (SqlComponent component) -> {
      component.setDataSource(dataSource);
    };
  }
  
  @Override
  public void configure() throws Exception {
    
    rest("/npa")
      .get("/")
        .produces("application/json")
        .to("direct:fetchNpas")
      .get("/by-state/{state}")
        .produces("application/json")
        .to("direct:fetchNpasByState")
      .get("/by-code/{code}")
        .produces("application/json")
        .to("direct:fetchNpasByCode")
      .post("/upload")
        .consumes("multipart/form-data")
        .produces("text/plain")
        .bindingMode(RestBindingMode.off)
        .to("direct:csvDataUpload")
    ;
    
    from("direct:fetchNpas")
      .log(LoggingLevel.DEBUG, log, "Fetching all NPAs")
      .to("sql:SELECT * FROM NPA")
      .transform().groovy("request.body.groupBy({ record -> record['STATE'] }).collect({ group -> ['state': group.key, 'codes': group.value*.get('CODE')] })")
      .marshal().json(JsonLibrary.Jackson)
    ;
    
    from("direct:fetchNpasByState")
      .log(LoggingLevel.DEBUG, log, "Fetching NPAs for state: [${header.state.toUpperCase()}]")
      .to("sql:SELECT CODE FROM NPA WHERE STATE=:#${header.state.toUpperCase()}")
      .filter(simple("${header.CamelSqlRowCount} <= 0"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant(null))
        .stop()
      .end()
      .transform().groovy("['codes': request.body*.get('CODE')]")
      .marshal().json(JsonLibrary.Jackson)
    ;
    
    from("direct:fetchNpasByCode")
      .log(LoggingLevel.DEBUG, log, "Fetching NPAs for area code: [${header.code}]")
      .to("sql:SELECT STATE FROM NPA WHERE CODE=:#${header.code}?outputType=SelectOne")
      .filter(simple("${header.CamelSqlRowCount} <= 0"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant(null))
        .stop()
      .end()
      .transform().groovy("['state': request.body?.toUpperCase()]")
      .marshal().json(JsonLibrary.Jackson)
    ;
    
    from("direct:csvDataUpload")
      .onException(Exception.class)
        .handled(true)
        .log(LoggingLevel.DEBUG, log, "Error inserting record for state=[${body?.get('state')}], code=[${body?.get('code')}], error=[${exception}]")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
        .setBody(simple("Not OK\n${exception}"))
      .end()
      .log(LoggingLevel.DEBUG, log, "Processing bulk CVS upload")
      .unmarshal().mimeMultipart()
      .convertBodyTo(byte[].class)
      .unmarshal().beanio("/beanio-mappings.xml", "npaCsvFile", "UTF-8")
      .log(LoggingLevel.DEBUG, log, "Inserting [${body[0].get('entries').size()}] records...")
      .split(simple("${body[0].get('entries')}"))
        .parallelProcessing()
        .to("direct:insertEntryIgnoringDuplicates")
      .end()
      .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
      .setBody(constant("OK"))
    ;
    
    from("direct:insertEntryIgnoringDuplicates")
      .onException(DuplicateKeyException.class)
        .handled(true)
        .log(LoggingLevel.DEBUG, log, "Got a duplicate: code=[${body['code'].trim()}], state=[${body['state'].trim()}]. Skipping...")
      .end()
      .to("sql:INSERT INTO NPA VALUES (:#${body['code'].trim()},:#${body['state'].trim()})")
    ;
  }
}
