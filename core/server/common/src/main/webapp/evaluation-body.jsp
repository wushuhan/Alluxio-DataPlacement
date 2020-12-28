    <%@ page import="java.util.*" %>
        <%@ page import="java.lang.*" %>
        <%@ page import="alluxio.web.*" %>
        <%@ page import="alluxio.wire.*" %>
        <%@ page import="java.io.File" %>
        <%@ page contentType="text/html;charset=UTF-8" language="java"  pageEncoding="UTF-8"  %>

        <jsp:include page="header-scripts.jsp" />
        <div class="container-fluid">
        <div class="row-fluid">
        <div class="accordion span6" id="accordion1">
        <div class="accordion-group">
        <div class="accordion-heading">
        <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion1" href="#data1">
        <h4>文件块访问时延</h4>
        </a>
        </div>
        <div id="data1" class="accordion-body collapse in">
        <div class="accordion-inner">
            <figure>
            <img class="img" src="img/BlockFetchingLatency.png" ?r=123 alt="Alluxio Logo"/>
            </figure>
        </div>
        </div>
        </div>
        </div>

            <div class="accordion span6" id="accordion2">
            <div class="accordion-group">
            <div class="accordion-heading">
            <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#data2">
            <h4>解码时延</h4>
            </a>
            </div>
            <div id="data2" class="accordion-body collapse in">
            <div class="accordion-inner">
            <figure>
            <img class="img" src="img/DecodingLatency.png" alt="Alluxio Logo"/>
            </figure>
            </div>
            </div>
            </div>
            </div>
            </div>
            <div class="row-fluid">
            <div class="accordion span6" id="accordion1">
            <div class="accordion-group">
            <div class="accordion-heading">
            <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion1" href="#data1">
            <h4>文件访问时延</h4>
            </a>
            </div>
            <div id="data1" class="accordion-body collapse in">
            <div class="accordion-inner">
            <figure>
            <img class="img" src="img/FileReadLatency.png" alt="Alluxio Logo"/>
            </figure>
            </div>
            </div>
            </div>
            </div>
            <% if (new File("/home/wsh/erasure_coding/alluxio-origin/core/server/common/src/main/webapp/img/Stragglers.png").exists()) { %>
            <div class="accordion span6" id="accordion2">
            <div class="accordion-group">
            <div class="accordion-heading">
            <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion2" href="#data2">
            <h4>掉队者</h4>
            </a>
            </div>
            <div id="data2" class="accordion-body collapse in">
            <div class="accordion-inner">
            <figure>
            <img class="img" src="img/Stragglers.png" alt="Alluxio Logo"/>
            </figure>
            </div>
            </div>
            </div>
            </div>
            <% } %>
            </div>
            </div>
