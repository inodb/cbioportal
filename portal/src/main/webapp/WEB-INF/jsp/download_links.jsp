<%@ page import="java.util.TreeSet" %>
<%@ page import="org.mskcc.cbio.portal.servlet.ShowData" %>
<%@ page import="org.mskcc.cbio.portal.model.DownloadLink" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<!--<div class="section" id="data_download"> -->
<h4>The following are tab-delimited data files:</h4>
<ul>
<%
    ArrayList<DownloadLink> downloadLinkList = (ArrayList<DownloadLink>)
            request.getSession().getAttribute(QueryBuilder.DOWNLOAD_LINKS);
    int i = 0;
    for (DownloadLink link:  downloadLinkList) {
        out.println ("<li><a href='show_data.do?" + ShowData.INDEX + "=" + i + "'>"
                + link.getProfile().getProfileName() +"</a>:  ["
                + link.getGeneList().size() + " genes]");
        out.println ("&nbsp;<a href='show_data.do?transpose_matrix=1&" + ShowData.INDEX + "=" + i + "'>" 
                + "[Transposed Matrix]</a></li>");
        i++;
    }
%>
</ul>
<!--</div>-->