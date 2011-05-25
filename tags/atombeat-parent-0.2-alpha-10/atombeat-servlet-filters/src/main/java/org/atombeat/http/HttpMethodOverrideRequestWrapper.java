/**
 * 
 */
package org.atombeat.http;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author aliman
 *
 */
public class HttpMethodOverrideRequestWrapper extends HttpServletRequestWrapper {

	public static final String HEADER_METHODOVERRIDE = "X-HTTP-Method-Override";
	private HttpServletRequest httpRequest;
	private Set<String> allowedOverrides;

	/**
	 * @param request
	 */
	public HttpMethodOverrideRequestWrapper(HttpServletRequest request, Set<String> allowedOverrides) {
		super(request);
		this.httpRequest = request;
		this.allowedOverrides = allowedOverrides;
	}
	
	@Override
	public String getMethod() {
		String methodOverride = httpRequest.getHeader(HEADER_METHODOVERRIDE);
		if (methodOverride != null && allowedOverrides.contains(methodOverride)) {
			return methodOverride;
		}
		return httpRequest.getMethod();
	}

}
