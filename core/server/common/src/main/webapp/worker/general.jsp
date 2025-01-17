<%--

    The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
    (the "License"). You may not use this work except in compliance with the License, which is
    available at www.apache.org/licenses/LICENSE-2.0

    This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied, as more fully set forth in the License.

    See the NOTICE file distributed with this work for information regarding copyright ownership.

--%>
<%@ page import="java.util.*" %>
<%@ page import="alluxio.util.*" %>
<%@ page import="alluxio.web.WebInterfaceWorkerGeneralServlet.UIStorageDir" %>
<%@ page import="alluxio.web.WebInterfaceWorkerGeneralServlet.UIUsageOnTier" %>
<%@ page import="alluxio.web.WebInterfaceWorkerGeneralServlet.UIWorkerInfo" %>
  <%@ page contentType="text/html;charset=UTF-8" language="java"  pageEncoding="UTF-8"  %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <jsp:include page="../header-links.jsp" />
</head>
<title>Alluxio</title>
<body>
<script src="../js/jquery-1.9.1.min.js" type="text/javascript"></script>
<script src="../js/bootstrap.min.js"></script>
<div class="container-fluid">
  <% request.setAttribute("useWorkerHeader", "1"); %>
  <% UIWorkerInfo workerInfo = (UIWorkerInfo) request.getAttribute("workerInfo"); %>
  <jsp:include page="/header" />
  <div class="row-fluid">
    <div class="accordion span6" id="accordion1">
      <div class="accordion-group">
        <div class="accordion-heading">
          <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion1" href="#data1">
            <h4>Worker节点摘要</h4>
          </a>
        </div>
        <div id="data1" class="accordion-body collapse in">
          <div class="accordion-inner">
            <table class="table">
              <tbody>
                <tr>
                  <th>Worker地址:</th>
                  <th><%= workerInfo.getWorkerAddress() %></th>
                </tr>
                <tr>
                  <th>开始时间:</th>
                  <th><%= workerInfo.getStartTime() %></th>
                </tr>
                <tr>
                  <th>运行时间:</th>
                  <th><%= workerInfo.getUptime() %></th>
                </tr>
<%--                <tr>--%>
<%--                  <th>版本:</th>--%>
<%--                  <!-- <th>${version}</th> -->--%>
<%--                  <th><%= request.getAttribute("version") %></th>--%>
<%--                </tr>--%>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="accordion span6" id="accordion2">
      <div class="accordion-group">
        <div class="accordion-heading">
          <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#data2">
            <h4>存储占用</h4>
          </a>
        </div>
        <div id="data2" class="accordion-body collapse in">
          <div class="accordion-inner">
            <table class="table">
              <tbody>
                <tr>
                  <th>总容量 / 已占用</th>
                  <th><%= request.getAttribute("capacityBytes") %> / <%= request.getAttribute("usedBytes") %></th>
                </tr>
                <% List<UIUsageOnTier> usageOnTiers = (List<UIUsageOnTier>) request.getAttribute("usageOnTiers"); %>
                <% for (UIUsageOnTier usageOnTier : usageOnTiers) { %>
                  <tr>
                    <th><%= usageOnTier.getTierAlias() %> 总容量 / 已占用</th>
                    <th>
                      <%= FormatUtils.getSizeFromBytes(usageOnTier.getCapacityBytes()) %> /
                      <%= FormatUtils.getSizeFromBytes(usageOnTier.getUsedBytes()) %>
                    </th>
                  </tr>
                <% } %>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="row-fluid">
    <div class="accordion span14" id="accordion3">
      <div class="accordion-group">
        <div class="accordion-heading">
          <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion3" href="#data3">
            <h4>存储层信息</h4>
          </a>
        </div>
        <div id="data3" class="accordion-body collapse in">
          <div class="accordion-inner">
            <table class="table table-hover table-condensed">
              <thead>
                <th>存储层</th>
                <th>路径</th>
                <th>容量</th>
                <th>已占用</th>
                <th>总容量</th>
              </thead>
              <tbody>
                <% for (UIStorageDir dir : ((List<UIStorageDir>) request.getAttribute("storageDirs"))) { %>
                  <tr>
                    <th><%= dir.getTierAlias() %></th>
                    <th><%= dir.getDirPath() %></th>
                    <th><%= FormatUtils.getSizeFromBytes(dir.getCapacityBytes()) %></th>
                    <th><%= FormatUtils.getSizeFromBytes(dir.getUsedBytes()) %></th>
                    <th>
                      <div class="progress custom-progress">
                        <% int usedSpacePercent = (int) (100.0 * dir.getUsedBytes() / dir.getCapacityBytes()); %>
                        <% int freeSpacePercent = 100 - usedSpacePercent; %>
                        <div class="bar bar-success" style="width: <%= freeSpacePercent %>%;">
                          <% if (freeSpacePercent >= usedSpacePercent) { %>
                            <%= freeSpacePercent %>%Free
                          <% } %>
                        </div>
                        <div class="bar bar-danger" style="width: <%= usedSpacePercent %>%;">
                          <% if (freeSpacePercent < usedSpacePercent) { %>
                            <%= usedSpacePercent %>%Used
                          <% } %>
                        </div>
                      </div>
                    </th>
                  </tr>
                <% } %>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
<%--  <%@ include file="../footer.jsp" %>--%>
</div>
</body>
</html>
