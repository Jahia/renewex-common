package org.jahia.se.modules.renewex;

import org.apache.commons.lang3.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.bin.filters.ServletWrappingFilter;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

/**
 * This proxy is used to call the cloudinary admin API from the piker
 * in order to get the asset_id of the selected element
 */
@Component(service = AbstractServletFilter.class, configurationPid="org.jahia.se.modules.renewex_common")
public class RenewexProxyServlet extends ServletWrappingFilter {

    private String token;

    @Activate
    public void onActivate(Map<String, String> params) {
        token = params.get("renewex.token");

        setServletName("renewex");
        setServletClass(URITemplateProxyServlet.class);

        Map<String, String> initParams = new HashMap<>();
        StringBuilder targetUri = new StringBuilder();
        targetUri.append(params.get("renewex.apiSchema"));
        targetUri.append("://");
        targetUri.append(params.get("renewex.apiEndPoint"));
        targetUri.append("/{_path}");

        initParams.put(ProxyServlet.P_TARGET_URI, targetUri.toString());
        initParams.put(ProxyServlet.P_LOG, "true");
        setInitParameters(initParams);

        Set<String> dispatcherTypes = new HashSet<>();
        dispatcherTypes.add("REQUEST");
        dispatcherTypes.add("FORWARD");
        setDispatcherTypes(dispatcherTypes);

        setUrlPatterns(new String[]{"/renewex/*"});
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        super.doFilter(new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equals(name)) {
                    return "Bearer " + token;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("Authorization".equals(name)) {
                    return Collections.enumeration(Collections.singleton("Bearer " + token));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("Authorization");
                return Collections.enumeration(names);
            }

            @Override
            public String getQueryString() {
                return "_path=" + StringUtils.substringAfter(((HttpServletRequest) servletRequest).getRequestURI(), "/renewex/");
            }
        }, servletResponse, filterChain);
    }
}
