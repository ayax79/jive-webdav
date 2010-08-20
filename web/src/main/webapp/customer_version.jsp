<%@ page import="java.util.Properties" %>
<%@ page import="com.jivesoftware.util.ClassUtilsImpl" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head><title>Customer Version</title></head>

  <%
      Properties versionProperties = new Properties();
      versionProperties.load(new ClassUtilsImpl().getResourceAsStream("/version.properties"));
  %>

  <body>
    CI Project Name = <%= versionProperties.getProperty("build.project.name") %><br/>
    CI Build Number = <%= versionProperties.getProperty("build.number") %><br/>
    CI Build VCS Number = <%= versionProperties.getProperty("build.vcs.number") %>
  </body>
</html>