<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<jsp:include page="/header.jspf"/>

<div id="wrapper" style="width: 99%;">
	<jsp:include page="membertoc.jspf"/>
	<td valign="top" colspan="3" class="bb">
		<%@page import="com.notsecurebank.model.Account"%>
		<%@page import="com.notsecurebank.util.ServletUtil"%>
		<div class="fl" style="width: 99%;">
		
		<%
           com.notsecurebank.model.User user = (com.notsecurebank.model.User)request.getSession().getAttribute("user");
		%>
		
		<h1>Hello <%= user.getFirstName() + " " + user.getLastName() %></h1>
		
		<p>
		  Welcome to the online banking system.
		</p>
		
		<form name="details" method="get" action="showAccount">
		<table border="0">
		  <TR valign="top">
		    <td>View Account Details:</td>
		    <td align="left">
			  <select size="1" name="listAccounts" id="listAccounts">
				<% 
				for (Account account: user.getAccounts()){
					out.println("<option value=\""+account.getAccountId()+"\" >" + account.getAccountId() + " " + account.getAccountName() + "</option>");
				}
				%>
			  </select>
		      <input type="submit" id="btnGetAccount" value="   GO   ">
		    </td>
		  </tr>
		
		</table>
		</form>
		
		</div>
    </td>
</div>

<jsp:include page="/footer.jspf"/>	