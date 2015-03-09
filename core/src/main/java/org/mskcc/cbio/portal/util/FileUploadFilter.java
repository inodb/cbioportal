/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center 
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center 
 * has been advised of the possibility of such damage.
*/

package org.mskcc.cbio.portal.util;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
* Filter that wraps an underlying file upload request. 
* 
* <P>This filter should be configured only for those operations that use a 
* file upload request.
*/
public final class FileUploadFilter<W extends HttpServletRequestWrapper> implements Filter {

  public void init(FilterConfig aConfig) throws ServletException {
    //do nothing
  }
  
  public void destroy() {
    //do nothing
  }
  
  public void doFilter(
   ServletRequest aRequest, ServletResponse aResponse, FilterChain aChain
  ) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) aRequest;
    if ( isFileUploadRequest(request) ) {
      FileUploadRequestWrapper wrapper = new FileUploadRequestWrapper(request);
      aChain.doFilter(wrapper, aResponse);
    }
    else {
      aChain.doFilter(aRequest, aResponse);
    }
  }
  
  private boolean isFileUploadRequest(HttpServletRequest aRequest){
    return     
      aRequest.getMethod().equalsIgnoreCase("POST") && 
      aRequest.getContentType().startsWith("multipart/form-data")
    ;
  }
}