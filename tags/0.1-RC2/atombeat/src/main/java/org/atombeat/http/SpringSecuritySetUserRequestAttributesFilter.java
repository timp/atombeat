/**
 * 
 */
package org.atombeat.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atombeat.http.HttpFilter;
import org.exist.xquery.value.ValueSequence;
import org.exist.xquery.value.StringValue;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;

/**
 * @author aliman
 *
 */
public class SpringSecuritySetUserRequestAttributesFilter extends HttpFilter {

	
	
	
	private Log log = LogFactory.getLog(this.getClass());
	
	
	
	public static final String USERNAMEREQUESTATTRIBUTEKEY = "user-name";
	public static final String USERROLESREQUESTATTRIBUTEKEY = "user-roles";
	public static final String USERROLESXMLREQUESTATTRIBUTEKEY = "user-roles-xml";
	
	
	
	
	@Override
	public void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		log.debug("request inbound");

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		String name = null;
		
		if (authentication != null) {
			
			name = authentication.getName();
			log.debug("found username: "+name);
			
			request.setAttribute(USERNAMEREQUESTATTRIBUTEKEY, new StringValue(name));
			
			GrantedAuthority[] authorities = authentication.getAuthorities();

			ValueSequence roles = new ValueSequence();
			
			for (GrantedAuthority a : authorities) {
				log.debug("found role: "+a.toString());
				StringValue s = new StringValue(a.toString());
				roles.add(s);
			}

			request.setAttribute(USERROLESREQUESTATTRIBUTEKEY, roles);
			
			String rolesXml = "<roles xmlns=''>";
			for (GrantedAuthority a : authorities) {
				rolesXml += "<role>" + a.toString() + "</role>";
			}
			rolesXml += "</roles>";

			request.setAttribute(USERROLESXMLREQUESTATTRIBUTEKEY, rolesXml);

		}
		
		HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
			
			@Override
			public String getRemoteUser() {
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				if (authentication != null) {
					return authentication.getName();
				}
				return null;
			};
			
			@Override
			public boolean isUserInRole(String role) {
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				if (authentication != null) {
					GrantedAuthority[] authorities = authentication.getAuthorities();
					for (GrantedAuthority a : authorities) {
						if (role.equals(a.toString()))
							return true;
					}
				}
				return false;
			}
			
		};
		
		chain.doFilter(wrappedRequest, response);

		log.debug("response outbound");
	}

}
