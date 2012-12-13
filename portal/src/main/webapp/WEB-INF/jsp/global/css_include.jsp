<%@ page import="org.mskcc.cbio.portal.util.Config" %><%
    Config globalConfig2 = Config.getInstance();
    String global_style = globalConfig2.getProperty("global_css");
    String special_style = globalConfig2.getProperty("special_css");
    if (global_style == null) {
        global_style = "css/global_portal.css";
    } else {
        global_style = "css/" + global_style;
    }
    if (special_style != null) {
        special_style = "css/" + special_style;
    }
%>

<!-- Include Global Style Sheets -->
<link rel="icon" href="http://cbio.mskcc.org/favicon.ico"/>
<link href="css/genomic.css" type="text/css" rel="stylesheet" />
<link href="css/popeye/jquery.popeye.css" type="text/css" rel="stylesheet" />
<link href="css/popeye/jquery.popeye.style.css" type="text/css" rel="stylesheet" />
<link href="css/tipTip.css" type="text/css" rel="stylesheet" />
<link href="css/jquery.qtip.min.css" type="text/css" rel="stylesheet" />
<link href="<%= global_style %>" type="text/css" rel="stylesheet" />
<% if (special_style != null) { %>
    <link href="<%= special_style %>" type="text/css" rel="stylesheet" />
<% } %>
<link href="css/redmond/jquery-ui-1.8.14.custom.css" type="text/css" rel="stylesheet" />
<link href="css/data_table.css" type="text/css" rel="stylesheet" />
<link href="css/data_table_jui.css" type="text/css" rel="stylesheet" />
<link href="css/data_table_ColVis.css" type="text/css" rel="stylesheet" />
<link href="css/custom_case_set.css" type="text/css" rel="stylesheet"/>
<link href="css/ui.dropdownchecklist.themeroller.css" type="text/css" rel="stylesheet"/>