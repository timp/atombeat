package org.atombeat.http;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.ValueSequence;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
			
			Collection<GrantedAuthority> authorities = authentication.getAuthorities();

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
