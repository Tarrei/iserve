package uk.ac.open.kmi.iserve.rest;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by Luca Panziera on 17/07/2014.
 */
public class RestFilter implements Filter {

    private RequestDispatcher defaultRequestDispatcher;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        defaultRequestDispatcher.forward(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.defaultRequestDispatcher =
                filterConfig.getServletContext().getNamedDispatcher("Discovery Endpoint");
    }
}
