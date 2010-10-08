package org.atombeat.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atombeat.http.HttpFilter;
import org.exist.xquery.value.ValueSequence;
import org.exist.xquery.value.StringValue;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;

public class MinimalSpringSecuritySetUserRequestAttributesFilter extends HttpFilter {

	public static final String USERNAMEREQUESTATTRIBUTEKEY = "user-name";
	public static final String USERROLESREQUESTATTRIBUTEKEY = "user-roles";
	
	@Override
	public void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		String name = null;
		
		if (authentication != null) {
			
			name = authentication.getName();
			
			request.setAttribute(USERNAMEREQUESTATTRIBUTEKEY, new StringValue(name));
			
			GrantedAuthority[] authorities = authentication.getAuthorities();

			ValueSequence roles = new ValueSequence();
			
			for (GrantedAuthority a : authorities) {
				StringValue s = new StringValue(a.toString());
				roles.add(s);
			}

			request.setAttribute(USERROLESREQUESTATTRIBUTEKEY, roles);
			
		}
		
		chain.doFilter(request, response);

	}

}
