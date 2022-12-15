package com.tth.common.log;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@AllArgsConstructor
@Log4j2
public class RequestResponseBodyLoggerAdvice implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

	private ObjectMapper objectMapper;
	private HttpServletRequest servletRequest;

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return true;
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
		return inputMessage;
	}

	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		Map<String, Object> requestData = new LinkedHashMap<>();
		requestData.put("sessionId", servletRequest.getSession().getId());
		requestData.put("method", servletRequest.getMethod());
		requestData.put("uri", servletRequest.getRequestURI());
		requestData.put("requestBody", body);
		log.debug(parseToJsonString(requestData));
		return body;
	}

	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {

		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
		
		boolean isErrorResponse = HttpStatus.valueOf(servletResponse.getStatus()).is5xxServerError();
		boolean isUnsuccessResponse = HttpStatus.valueOf(servletResponse.getStatus()).is2xxSuccessful() == false;
		boolean isGetMethod = servletRequest.getMethod().equals(HttpMethod.GET.name());
		
		Level level = null;
		if (isErrorResponse) {
			level = Level.ERROR;
		} else if (isUnsuccessResponse) {
			level = Level.WARN;
		} else if (isGetMethod == false) {
			level = Level.DEBUG;
		} else if (isGetMethod) {
			level = Level.TRACE;
		}
		
		Map<String, Object> responseData = new LinkedHashMap<>();
		responseData.put("sessionId", servletRequest.getSession().getId());
		responseData.put("method", servletRequest.getMethod());
		responseData.put("uri", servletRequest.getRequestURI());
		responseData.put("status", servletResponse.getStatus());
		responseData.put("responseBody", body);
		log.log(level, parseToJsonString(responseData));
		
		return body;
	}

	private String parseToJsonString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException ex) {
			ex.printStackTrace();
			return object.toString();
		}
	}

}
