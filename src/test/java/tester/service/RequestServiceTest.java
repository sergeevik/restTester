package tester.service;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.internal.RequestLogSpecificationImpl;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.protocol.ResponseServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import tester.exeption.ExecuteFail;
import tester.model.*;
import tester.model.Properties;

import java.io.File;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestServiceTest {

    private RequestSpecification mock;

    @Before
    public void setUp() {
        mock = mock(RequestSpecification.class);
    }

    @After
    public void after(){
        verifyNoMoreInteractions(mock);
    }

    @Test
    public void execute() {
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                );
        RequestService requestService = new RequestService(request, new Properties());
        RequestService requestServiceSpy = spy(requestService);
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);
        doReturn(response).when(requestServiceSpy).executeRequest(any(RequestSpecification.class));
        requestServiceSpy.execute();

        verify(requestServiceSpy, times(1)).fillBody(any(RequestSpecification.class));
        verify(requestServiceSpy, times(1)).fillContentType(any(RequestSpecification.class));
        verify(requestServiceSpy, times(1)).fillHeaders(any(RequestSpecification.class));
        verify(requestServiceSpy, times(1)).fillQueryParam(any(RequestSpecification.class));

        verify(requestServiceSpy, times(1)).executeRequest(any(RequestSpecification.class));
        verify(requestServiceSpy, times(1)).checkResponseHeaders(any(Response.class));
        verify(requestServiceSpy, times(1)).checkResponseEqualsScheme(any(Response.class), anyString());
        verify(requestServiceSpy, times(1)).checkResponseCode(any(Response.class));
        verify(requestServiceSpy, times(1)).checkExpectedResult(any(Response.class));

    }

    @Test(expected = ExecuteFail.class)
    public void executeFail() {
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                );
        RequestService requestService = new RequestService(request, new Properties());
        RequestService requestServiceSpy = spy(requestService);
        Response response = mockResponse();
        ValidatableResponse validatableResponse = mock(ValidatableResponse.class, invocationOnMock -> {
            throw new AssertionError();
        });
        when(response.then()).thenReturn(validatableResponse);
        doReturn(response).when(requestServiceSpy).executeRequest(any(RequestSpecification.class));
        requestServiceSpy.execute();
    }

    @Test
    public void fillBodyIfExist() {
        Request request = new Request().withBody("body");
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillBody(mock);
        verify(mock, times(1)).body("body");
    }
    @Test
    public void notFillBodyIfNoExist() {
        Request request = new Request();
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillBody(mock);
        verify(mock, Mockito.never()).body(anyString());
    }

    @Test
    public void fillOneQueryParamIfExist() {
        QueryParam queryParam = new QueryParam()
                .withKey("key")
                .withValue("value");
        Request request = new Request()
                .withQueryParams(Collections.singletonList(queryParam));
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillQueryParam(mock);
        verify(mock, times(1)).queryParam(queryParam.getKey(), queryParam.getValue());
    }
    @Test
    public void fillListQueryParamIfExist() {

        QueryParam firstQueryParam = new QueryParam()
                .withKey("key1")
                .withValue("value1");
        QueryParam secondQueryParam = new QueryParam()
                .withKey("key2")
                .withValue("value2");

        List<QueryParam> queryParams = Arrays.asList(firstQueryParam, secondQueryParam);
        Request request = new Request().withQueryParams(queryParams);
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillQueryParam(mock);
        for (QueryParam queryParam : queryParams) {
            verify(mock, times(1))
                    .queryParam(queryParam.getKey(), queryParam.getValue());
        }
    }
    @Test
    public void notFillQueryParamIfNoExist() {
        Request request = new Request();
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillQueryParam(mock);
        verify(mock, Mockito.never()).queryParam(anyString(), anyString());
    }

    @Test
    public void fillOneHeaderIfExist() {
        Header header = new Header()
                .withKey("key")
                .withValue("value");
        Request request = new Request()
                .withHeaders(Collections.singletonList(header));
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillHeaders(mock);
        verify(mock, times(1)).header(header.getKey(), header.getValue());
    }
    @Test
    public void fillListHeaderIfExist() {
        Header firstHeader = new Header()
                .withKey("key1")
                .withValue("value1");
        Header secondHeader = new Header()
                .withKey("key2")
                .withValue("value2");

        List<Header> headers = Arrays.asList(firstHeader, secondHeader);
        Request request = new Request().withHeaders(headers);
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillHeaders(mock);
        for (Header header : headers) {
            verify(mock, times(1))
                    .header(header.getKey(), header.getValue());
        }
    }
    @Test
    public void notFillHeaderIfNoExist() {
        Request request = new Request();
        RequestService requestService = new RequestService(request, new Properties());
        requestService.fillHeaders(mock);
        verify(mock, Mockito.never()).header(anyString(), anyString());
    }

    @Test
    public void fillContentTypeFromRequest() {
        Request request = new Request().withContentType("req json");
        Properties properties = new Properties().withContentType("prop json");
        RequestService requestService = new RequestService(request, properties);
        requestService.fillContentType(mock);
        verify(mock, times(1)).contentType("req json");
    }
    @Test
    public void fillContentTypeFromProperties() {
        Request request = new Request();
        Properties properties = new Properties().withContentType("prop json");
        RequestService requestService = new RequestService(request, properties);
        requestService.fillContentType(mock);
        verify(mock, times(1)).contentType("prop json");
    }

    @Test
    public void executePost() {
        Request request = new Request()
                .withRequestType(Request.RequestType.POST)
                .withRelativeUrl("post");
        RequestService requestService = new RequestService(request, new Properties());
        requestService.executeRequest(mock);
        verify(mock, times(1)).post("post");
    }
    @Test
    public void executeGet() {
        Request request = new Request()
                .withRequestType(Request.RequestType.GET)
                .withRelativeUrl("get");
        RequestService requestService = new RequestService(request, new Properties());
        requestService.executeRequest(mock);
        verify(mock, times(1)).get("get");
    }
    @Test
    public void executeGetAsDefault() {
        Request request = new Request()
                .withRelativeUrl("get");
        RequestService requestService = new RequestService(request, new Properties());
        requestService.executeRequest(mock);
        verify(mock, times(1)).get("get");
    }

    @Test
    public void expectedResultCheckSuccess() {
        Request request = new Request().withResponseValidator(
                new ResponseValidator()
                .withExpectedResult(
                        new ExpectedResult("#key#", true, "value")
                )
        );
        Properties properties = new Properties();
        RequestService requestService = new RequestService(request, properties);
        Response mockResp = mockResponse();
        requestService.checkExpectedResult(mockResp);

    }

    @Test
    public void whenExpectedResultNullResultSuccess() {
        Request request = new Request().withResponseValidator(
                new ResponseValidator()
        );
        Properties properties = new Properties();
        RequestService requestService = new RequestService(request, properties);
        Response mockResp = mockResponse();
        requestService.checkExpectedResult(mockResp);
    }
    @Test(expected = AssertionError.class)
    public void whenExpectedResultNotEqual() {
        Request request = new Request().withResponseValidator(
                new ResponseValidator()
                        .withExpectedResult(
                                new ExpectedResult("#key#", true, "fail")
                        )
        );
        Properties properties = new Properties();
        RequestService requestService = new RequestService(request, properties);
        Response mockResp = mockResponse();
        requestService.checkExpectedResult(mockResp);
    }
    @Test(expected = AssertionError.class)
    public void whenExpectedResultNotContainsInResponse() {
        Request request = new Request().withResponseValidator(
                new ResponseValidator()
                        .withExpectedResult(
                                new ExpectedResult("#error#", true, "fail")
                        )
        );
        Properties properties = new Properties();
        RequestService requestService = new RequestService(request, properties);
        Response mockResp = mockResponse();
        requestService.checkExpectedResult(mockResp);
    }

    @Test
    public void testSchemaFullPathInRequest() {
        String fullPath = "fullPath";
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                                .withSchemaFullPath(fullPath)
                );

        RequestService requestService = new RequestService(request, new Properties());
        String schemaPath = requestService.getSchemaPath();
        assertThat(schemaPath)
                .isNotNull()
                .isEqualTo(fullPath);
    }


    @Test
    public void testSchemaRelativePathInRequestThenGetParentPathFromProperties() {
        String parentPath = "parentPath";
        String relativePath = "relativePath";
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                                .withSchemaRelativePath(relativePath)
                );

        Properties properties = new Properties()
                .withSchemaBaseUrl(parentPath);
        RequestService requestService = new RequestService(request, properties);
        String schemaPath = requestService.getSchemaPath();
        assertThat(schemaPath)
                .isNotNull()
                .isEqualTo(parentPath + relativePath);
    }
    @Test
    public void testSchemaRelativePathInRequestEmptyAndParentEmptyThenReturnEmptyString() {
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                                .withSchemaRelativePath(null)
                );

        Properties properties = new Properties()
                .withSchemaBaseUrl(null);
        RequestService requestService = new RequestService(request, properties);
        String schemaPath = requestService.getSchemaPath();
        assertThat(schemaPath)
                .isNotNull()
                .isEmpty();
    }
    @Test
    public void testSchemaPathEmptyWhenResponseValidatorNull() {
        Request request = new Request();

        Properties properties = new Properties();
        RequestService requestService = new RequestService(request, properties);
        String schemaPath = requestService.getSchemaPath();
        assertThat(schemaPath)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void testCheckResponseCodeDefault200() {
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);
        RequestService requestService = new RequestService(new Request(), new Properties());
        requestService.checkResponseCode(response);
        verify(validatableResponse, times(1)).statusCode(200);
    }
    @Test
    public void testCheckResponseCodeSetCustom() {
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);
        Request request = new Request()
                .withAnswerCode(1100);
        RequestService requestService = new RequestService(request, new Properties());
        requestService.checkResponseCode(response);
        verify(validatableResponse, times(1)).statusCode(1100);
    }


    @Test
    public void testCheckSchemaWhenSchemaValidateFlagFalseByDefault() {
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);

        Request request = new Request()
                .withResponseValidator(new ResponseValidator());
        RequestService requestService = new RequestService(request, new Properties());
        requestService.checkResponseEqualsScheme(response, "");
        verify(validatableResponse, never()).body(any());
    }

    @Test
    public void testCheckSchemaWhenSchemaValidateFlagTrue() {
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);

        Request request = new Request()
                .withResponseValidator(new ResponseValidator().withValidateSchema(true));
        RequestService requestService = new RequestService(request, new Properties());
        requestService.checkResponseEqualsScheme(response, "");
        verify(validatableResponse, times(1)).body(any());
    }

    @Test
    public void checkResponseHeaderWhenHeaderSet() {
        List<Header> headers = Collections.singletonList(new Header("key", "value"));
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                                .withHeaders(headers)
                );
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);

        RequestService requestService = new RequestService(request, new Properties());
        requestService.checkResponseHeaders(response);
        for (Header header : headers) {
            verify(validatableResponse, times(1)).header(header.getKey(), header.getValue());
        }
    }
    @Test
    public void checkResponseHeaderWhenNoHeader() {
        Request request = new Request()
                .withResponseValidator(
                        new ResponseValidator()
                                .withHeaders(Collections.emptyList())
                );
        Response response = mockResponse();
        ValidatableResponse validatableResponse = validateRespMock();
        when(response.then()).thenReturn(validatableResponse);

        RequestService requestService = new RequestService(request, new Properties());
        requestService.checkResponseHeaders(response);
            verify(validatableResponse, never()).header(anyString(), anyString());
    }

    private Response mockResponse() {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("key", "value");
        JsonPath jsonPathMock = mock(JsonPath.class);
        when(jsonPathMock.get()).thenReturn(map);
        Response mockResp = mock(Response.class);
        when(mockResp.jsonPath()).thenReturn(jsonPathMock);
        return mockResp;
    }

    private ValidatableResponse validateRespMock() {
        ValidatableResponse validateRespMock = mock(ValidatableResponse.class);
        when(validateRespMock.assertThat()).thenReturn(validateRespMock);
        return validateRespMock;
    }
}