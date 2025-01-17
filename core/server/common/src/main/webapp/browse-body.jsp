<%--

    The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
    (the "License"). You may not use this work except in compliance with the License, which is
    available at www.apache.org/licenses/LICENSE-2.0

    This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied, as more fully set forth in the License.

    See the NOTICE file distributed with this work for information regarding copyright ownership.

--%>
<%@ page import="java.util.*" %>
<%@ page import="alluxio.web.*" %>
<%@ page import="static org.apache.commons.lang.StringEscapeUtils.escapeHtml" %>
<%@ page import="static java.net.URLEncoder.encode" %>
  <%@ page contentType="text/html;charset=UTF-8" language="java"  pageEncoding="UTF-8"  %>

<jsp:include page="header-scripts.jsp" />

<script>
  var currentDir = "<%= request.getAttribute("currentPath").toString() %>";
  function getInputPath() {
    var path = $.trim($("#pathInput").val());
    return path;
  }

  function changeDir() {
    var path = getInputPath();
    path = encodeURI(path);
    window.location.href = "./browse?path=" + path;
  }

  $(document).ready(function () {
    if (base !== "./browse") {
      $("#pathNav").hide();
    } else {
      // set currentDir as default value for #pathInput.
      $("#pathInput").val(currentDir);
      // when clicking #goBtn or enter the return key, change directory.
      $("#goBtn").click(changeDir);
      $("#pathInput").keydown(function (e) {
        if (e.keyCode === 13) {
          changeDir();
        }
      });
    }
  });
</script>
<div class="container-fluid">
  <jsp:include page="/header" />

  <div class="container-fluid">
    <div class="row-fluid">
      <div class="span12 well">
        <h1 class="text-error">
          <%= request.getAttribute("invalidPathError") %>
        </h1>
        <div class="navbar">
          <div class="navbar-inner">
            <ul class="nav nav-pills">
              <% if (request.getAttribute("pathInfos") != null) { %>
                <% for (UIFileInfo pathInfo : ((UIFileInfo[]) request.getAttribute("pathInfos"))) { %>
                  <li><a href="./browse?path=<%= encode(pathInfo.getAbsolutePath(), "UTF-8") %>"><%= escapeHtml(pathInfo.getName()) %> </a></li>
                <% } %>
              <% } %>
              <% if (request.getAttribute("currentDirectory") != null) { %>
                <li class="active"><a href="./browse?path=<%= encode(request.getAttribute("currentPath").toString(), "UTF-8") %>"><%= escapeHtml(((UIFileInfo) request.getAttribute("currentDirectory")).getName()) %></a></li>
              <% } %>
            </ul>
            <div id="pathNav" class="input-append pull-right" style="margin: 5px; margin-right: 45px;width:600px; ">
              <input class="span12" id="pathInput" type="text" placeholder="Navigate to a directory">
              <button class="btn" id="goBtn" type="button">Go</button>
            </div>
          </div>
        </div>
        <table class="table table-condensed">
          <thead>
            <th>文件名</th>
            <th>文件大小</th>
            <th>块大小</th>
            <th>Alluxio存储</th>
            <% if ((Boolean)request.getAttribute("showPermissions")) { %>
              <th>权限</th>
              <th>所有者</th>
              <th>组</th>
            <% } %>
            <th>持久化状态</th>
            <th>已固定</th>
            <th>创建时间</th>
            <th>修改时间</th>
            <th>存放节点</th>
          <!--
            <c:if test = "${debug}">
              <th>[D]Inode Number</th>
              <th>[D]Checkpoint Path</th>
            </c:if>
          -->
            <% if ((Boolean) request.getAttribute("debug")) { %>
              <th>[D]DepID</th>
              <th>[D]INumber</th>
              <th>[D]UnderfsPath</th>
            <% } %>
  </thead>
          <tbody>
            <!--
            <c:forEach var="fileInfo" items="${fileInfos}">
              <tr>
                <th>
                  <c:if test = "${fileInfo.isDirectory}">
                    <i class="icon-folder-close"></i>
                  </c:if>
                  <c:if test = "${not fileInfo.isDirectory}">
                    <i class="icon-file"></i>
                  </c:if>
                  <a href="./browse?path=${fileInfo.absolutePath}"><c:out value="${fileInfo.name}"/></a>
                </th>
                <th>${fileInfo.size} Bytes</th>
                <th>${fileInfo.blockSizeBytes}</th>
                <th>
                  <c:if test = "${fileInfo.inAlluxio}">
                    <i class="icon-hdd"></i>
                  </c:if>
                  <c:if test = "${not fileInfo.inAlluxio}">
                    <i class="icon-hdd icon-white"></i>
                  </c:if>
                </th>
                <th>${fileInfo.creationTime}</th>
                <c:if test = "${debug}">
                  <th>${fileInfo.id}</th>
                  <th>${fileInfo.checkpointPath}</th>
                  <th>
                  <c:forEach var="location" items="${fileInfo.fileLocations}">
                    ${location}<br/>
                  </c:forEach>
                  </th>
                </c:if>
              </tr>
            </c:forEach>
          -->
            <% if (request.getAttribute("fileInfos") != null) { %>
              <% for (UIFileInfo fileInfo : ((List<UIFileInfo>) request.getAttribute("fileInfos"))) { %>
                <tr>
                  <th>
                    <% if (fileInfo.getIsDirectory()) { %>
                      <i class="icon-folder-close"></i>
                    <% } %>
                    <% if (!fileInfo.getIsDirectory()) { %>
                      <i class="icon-file"></i>
                    <% } %>
                    <a href="<%= (request.getAttribute("baseUrl") == null) ? "./browse" : request.getAttribute("baseUrl").toString() %>?path=<%=encode(fileInfo.getAbsolutePath(), "UTF-8")%>"><%= escapeHtml(fileInfo.getName()) %></a>
                  </th>
                  <th><%= fileInfo.getSize() %></th>
                  <th><%= fileInfo.getBlockSizeBytes() %></th>
                  <th>
                    <% if (fileInfo.getIsDirectory()) { %>
                    <% } %>
                    <% if (!fileInfo.getIsDirectory()) { %>
                      <% if (fileInfo.getInAlluxio()) { %>
                        <i class="icon-hdd"></i>
                      <% } %>
                      <% if (!fileInfo.getInAlluxio()) { %>
                        <i class="icon-hdd icon-white"></i>
                      <% } %>
                      <%= fileInfo.getInAlluxioPercentage() %>%
                    <% } %>
                  </th>
                  <% if ((Boolean)request.getAttribute("showPermissions")) { %>
                    <th><%= fileInfo.getMode() %></th>
                    <th><%= fileInfo.getOwner() %></th>
                    <th><%= fileInfo.getGroup() %></th>
                  <% } %>
                  <th><%= (fileInfo.getPersistenceState()) %></th>
                  <th><%= (fileInfo.isPinned() ? "YES" : "NO") %></th>
                  <th><%= fileInfo.getCreationTime() %></th>
                  <th><%= fileInfo.getModificationTime() %></th>
                  <th>
                  <% for (String location : fileInfo.getFileLocations()) { %>
                    <%= location+" " %>
                    <% } %>
                  </th>
                  <% if ((Boolean) request.getAttribute("debug")) { %>
                    <th><%= fileInfo.getId() %></th>

                  <% } %>
                </tr>
              <% } %>
            <% } %>
          </tbody>
        </table>

        <%@ include file="pagination-component.jsp" %>

      </div>
    </div>
  </div>
<%--  <%@ include file="footer.jsp" %>--%>
</div>

<%@ include file="browse-pagination-header.jsp" %>
<%@ include file="pagination-control.jsp" %>
