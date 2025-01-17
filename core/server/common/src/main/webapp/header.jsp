<%--

    The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
    (the "License"). You may not use this work except in compliance with the License, which is
    available at www.apache.org/licenses/LICENSE-2.0

    This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied, as more fully set forth in the License.

    See the NOTICE file distributed with this work for information regarding copyright ownership.

--%>
  <%@ page contentType="text/html;charset=UTF-8" language="java"  pageEncoding="UTF-8"  %>
<div class="navbar">
  <div class="navbar-inner">
    <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
      <span class="icon-bar"></span>
      <span class="icon-bar"></span>
      <span class="icon-bar"></span>
    </a>
    <a href="http://alluxio.org/" target="_blank" class="brand" ><img style="height:25px;margin-top:-5px;" src="img/Logo_my.png" alt="Alluxio Logo"/></a>
    <div class="nav-collapse collapse">
      <ul class="nav nav-pills">
        <li id="home-li"><a href="./home">首页</a></li>
        <% if (request.getAttribute("useWorkerHeader") == null) { %>
          <li id="browse-li"><a href="./browse?path=/">文件</a></li>
          <li id="metricsui-li"><a href="./metricsui">数据统计</a></li>
<%--          <li id="configuration-li"><a href="./configuration">Configuration</a></li>--%>
<%--          <li id="workers-li"><a href="./workers">存储节点</a></li>--%>
<%--          <li id="memory-li"><a href="./memory">In-Alluxio Data</a></li>--%>
        <% } else {%>
          <li id="blockInfo-li"><a href="./blockInfo">块信息</a></li>
        <% } %>
<%--        <li id="browseLogs-li"><a href="./browseLogs">Logs</a></li>--%>
<%--        <li id="evaluation-li"><a href="./evaluation">实验结果</a></li>--%>
<%--        <li id="autorefresh-li"><a href="javascript:toggleAutoRefresh();" id="autorefresh-link">Enable Auto-Refresh</a></li>--%>
<%--        <% if (request.getAttribute("useWorkerHeader") != null) { %>--%>
<%--          <li id="returnmaster-li"><a href="http://<%=request.getAttribute("masterHost") %>:<%= request.getAttribute("masterPort") %>">Return to Master</a></li>--%>
<%--        <% } %>--%>
      </ul>
    </div>
  </div>
</div>
<script>
  var url = window.location.pathname;
  $("#"+url.substring(1)+"-li").addClass("active");
</script>
