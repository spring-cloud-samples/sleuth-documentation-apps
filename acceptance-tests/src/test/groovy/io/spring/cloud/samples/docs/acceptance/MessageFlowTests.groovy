/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.cloud.samples.docs.acceptance

import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.spring.cloud.samples.docs.acceptance.common.tech.ExceptionLoggingRestTemplate
import io.spring.cloud.samples.docs.acceptance.common.tech.SpanUtil
import io.spring.cloud.samples.docs.acceptance.common.tech.TestConfiguration
import org.awaitility.core.ThrowingRunnable
import org.junit.jupiter.api.Test
import zipkin2.Span
import zipkin2.codec.SpanBytesDecoder

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@SpringBootTest(classes = TestConfiguration)
@Slf4j
@CompileStatic
class MessageFlowTests {

	public static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId"
	public static final String SPAN_ID_NAME = "X-B3-SpanId"
	private static final List<String> APP_NAMES = ['service1', 'service2', 'service3', 'service4']

	@Value('${service1.address:http://localhost:8081}')
	String service1Url
	@Value('${zipkin.query.port:9411}')
	Integer zipkinQueryPort
	@Value('${LOCAL_URL:http://localhost}')
	String zipkinQueryUrl

	@Test
	void 'should send message to service1 and receive combined response for traceId'() {
		String traceId = SpanUtil.idToHex(new Random().nextLong())
		//given: "Request with a traceId"
		RequestEntity request = request_to_service1(traceId)
		//when: "Request is sent to the Service1"
		request_sent_for_service1_with_traceId(request)
		//then: "Entry in Zipkin is present for the traceId"
		entry_for_trace_id_is_present_in_Zipkin(traceId)
		//and: "The dependency graph looks like in the docs"
		dependency_graph_is_correct()
	}

	@Test
	void 'should send message to service1 and get read timeout'() {
		String traceId = SpanUtil.idToHex(new Random().nextLong())
		//given: "Request with a traceId"
		RequestEntity request = request_to_service1_at_readtimeout(traceId)
		//when: "Failing request is sent to the Service1"
		failing_request_sent_for_service1_with_traceId(request)
		//then: "Entry in Zipkin is present for the traceId"
		failed_entry_for_trace_id_is_present_in_Zipkin(traceId)
	}

	@CompileStatic
	private void request_sent_for_service1_with_traceId(RequestEntity request) {
		await().pollInterval(1, SECONDS).atMost(60, SECONDS).untilAsserted(new ThrowingRunnable() {
			@Override
			void run() {
				ResponseEntity<String> service1Response = restTemplate().exchange(request, String)
				log.info("Response from service1Response is [$service1Response]")
				assert service1Response != null
				assert service1Response.statusCode == HttpStatus.OK
				assert service1Response.body.contains('Hello from service2, response from service3 [Hello from service3] and from service4 [Hello from service4]')
				log.info("The Sleuth Docs apps are working! Let's be happy!")
			}
		})
	}

	@CompileStatic
	private failing_request_sent_for_service1_with_traceId(RequestEntity request) {
		await().pollInterval(1, SECONDS).atMost(60, SECONDS).untilAsserted(new ThrowingRunnable() {
			@Override
			void run() {
				ResponseEntity<String> service1Response = restTemplate().exchange(request, String)
				log.info("Response from service1Response is [$service1Response]")
				assert service1Response != null
				assert service1Response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR || service1Response.body.contains('''"status":500,"error":"Internal Server Error"''')
			}
		})
	}

	private RequestEntity request_to_service1(String traceId) {
		HttpHeaders headers = new HttpHeaders()
		headers.add(SPAN_ID_NAME, traceId)
		headers.add(TRACE_ID_HEADER_NAME, traceId)
		URI uri = URI.create(wrapWithProtocolIfPresent("$service1Url/start"))
		RequestEntity requestEntity = new RequestEntity<>(headers, HttpMethod.POST, uri)
		log.info("Request with traceid [$traceId] to service1 [$requestEntity] is ready")
		return requestEntity
	}

	private RequestEntity request_to_service1_at_readtimeout(String traceId) {
		HttpHeaders headers = new HttpHeaders()
		headers.add(SPAN_ID_NAME, traceId)
		headers.add(TRACE_ID_HEADER_NAME, traceId)
		URI uri = URI.create(wrapWithProtocolIfPresent("$service1Url/readtimeout"))
		RequestEntity requestEntity = new RequestEntity<>(headers, HttpMethod.POST, uri)
		log.info("Request with traceid [$traceId] to service1 [$requestEntity] is ready")
		return requestEntity
	}

	private void entry_for_trace_id_is_present_in_Zipkin(String traceId) {
		await().pollInterval(1, SECONDS).atMost(60, SECONDS).untilAsserted(new ThrowingRunnable() {
			@Override
			@CompileDynamic
			void run() {
				ResponseEntity<String> response = checkStateOfTheTraceId(traceId)
				log.info("Response from the Zipkin query service about the trace id [$response] for trace with id [$traceId]")
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				log.info("Checking spans")
				List<Span> spans = SpanBytesDecoder.JSON_V2.decodeList(response.body.bytes)
				List<String> serviceNamesNotFoundInZipkin = serviceNamesNotFoundInZipkin(spans)
				log.info("The following services were not found in Zipkin $serviceNamesNotFoundInZipkin")
				assert serviceNamesNotFoundInZipkin.empty
				log.info("Zipkin tracing is working! Sleuth is working! Let's be happy!")
			}

			@CompileDynamic
			private List<String> serviceNamesNotFoundInZipkin(List<Span> spans) {
				List<String> remoteServiceName = spans.collect {
					it.remoteServiceName()
				}.flatten().unique()
				List<String> localServiceName = spans.collect {
					it.localServiceName()
				}.flatten().unique()
				return (APP_NAMES - remoteServiceName - localServiceName)
			}
		})
	}

	private void failed_entry_for_trace_id_is_present_in_Zipkin(String traceId) {
		await().pollInterval(1, SECONDS).atMost(60, SECONDS).untilAsserted(new ThrowingRunnable() {
			@Override
			@CompileDynamic
			void run() {
				ResponseEntity<String> response = checkStateOfTheTraceId(traceId)
				log.info("Response from the Zipkin query service about the trace id [$response] for trace with id [$traceId]")
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				List<Span> spans = SpanBytesDecoder.JSON_V2.decodeList(response.body.bytes)
				// we're checking if the latest annotation based functionality is working
				Span foundSpan = spans.find {
					it.name() == "first_span" && it.tags().find { it.key == "someTag" } &&
							// Brave || Otel
							it.tags().find { it.key == "error" } || it.annotations().find { it.value() == "exception" }
				}
				log.info("The following spans <{}> were found in Zipkin for the traceid <{}>", spans, traceId)
				log.info("Span with name [first_span] found? [" + foundSpan + "]")
				assert foundSpan != null
				log.info("Zipkin tracing is working! Sleuth is working! Let's be happy!")
			}
		})
	}

	private String parsedZipkinQuery() {
		return zipkinQueryUrl.split(" ")[0]
	}

	private ResponseEntity<String> checkStateOfTheTraceId(String traceId) {
		URI uri = URI.create("${wrapQueryWithProtocolIfPresent() ?: parsedZipkinQuery()}:${zipkinQueryPort}/api/v2/trace/$traceId")
		HttpHeaders headers = new HttpHeaders()
		log.info("Sending request to the Zipkin query service [$uri]. Checking presence of trace id [$traceId]")
		return new ExceptionLoggingRestTemplate().exchange(
				new RequestEntity<>(headers, HttpMethod.GET, uri), String
		)
	}

	@CompileDynamic
	private void dependency_graph_is_correct() {
		await().pollInterval(1, SECONDS).atMost(60, SECONDS).untilAsserted(new ThrowingRunnable() {
			@Override
			void run() {
				ResponseEntity<String> response = checkDependencies()
				log.info("Response from the Zipkin query service about the dependencies [$response]")
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				Map<String, List<String>> parentsAndChildren = [:]
				new JsonSlurper().parseText(response.body).inject(parentsAndChildren) { Map<String, String> acc, def json ->
					def list = acc[json.parent] ?: []
					list << json.child
					acc.put(json.parent, list)
					return acc
				}
				assert parentsAndChildren['service1'] == ['service2']
				assert parentsAndChildren['service2'].containsAll(['service3', 'service4'])
			}
		})
	}

	private ResponseEntity<String> checkDependencies() {
		URI uri = URI.create(wrapWithProtocolIfPresent("${wrapQueryWithProtocolIfPresent() ?: parsedZipkinQuery()}:${zipkinQueryPort}/api/v2/dependencies?endTs=${System.currentTimeMillis()}"))
		HttpHeaders headers = new HttpHeaders()
		log.info("Sending request to the Zipkin query service [$uri]. Checking the dependency graph")
		return new ExceptionLoggingRestTemplate().exchange(
				new RequestEntity<>(headers, HttpMethod.GET, uri), String
		)
	}

	private String wrapQueryWithProtocolIfPresent() {
		String zipkinUrlFromEnvs = System.getenv('spring.zipkin.query.url')
		if (zipkinUrlFromEnvs) {
			zipkinUrlFromEnvs = zipkinUrlFromEnvs.split(" ")[0]
			return "https://$zipkinUrlFromEnvs"
		}
		return zipkinUrlFromEnvs
	}

	private String wrapWithProtocolIfPresent(String url) {
		if (!url.startsWith("http")) {
			return "http://$url"
		}
		return url
	}

	private RestTemplate restTemplate() {
		return new ExceptionLoggingRestTemplate()
	}
}
