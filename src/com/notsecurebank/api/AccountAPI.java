package com.notsecurebank.api;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.notsecurebank.model.Account;
import com.notsecurebank.model.Transaction;
import com.notsecurebank.model.User;
import com.notsecurebank.util.DBUtil;
import com.notsecurebank.util.ServletUtil;

@Path("/account")
public class AccountAPI extends NotSecureBankAPI {

    private static final Logger LOG = LogManager.getLogger(AccountAPI.class);

    @GET
    @Produces("application/json")
    public Response getAccounts(@Context HttpServletRequest request) {
        LOG.info("getAccounts");

        String response;

        if (!ServletUtil.isLoggedin(request)) {
            response = "{\"loggedIn\" : \"false\"}";
            return Response.status(401).entity(response).build();
        }

        try {
            Account[] account = (ServletUtil.getUser(request)).getAccounts();
            response = "{\"Accounts\":\n[\n";
            for (int i = 0; i < account.length; i++) {
                response = response + "{ \"Name\" : \"" + account[i].getAccountName() + "\", \"id\": \"" + account[i].getAccountId() + "\"}";
                if (i < account.length - 1)
                    response = response + ",\n";
            }
            response = response + "\n]}";

        } catch (Exception e) {
            LOG.error(e.toString());
            response = "{\"Error\": \"Unknown error encountered\"}";
            return Response.status(501).entity(response).build();
        }

        return Response.status(200).entity(response).build();
    }

    @GET
    @Path("/{accountNo}")
    @Produces("application/json")
    public Response getAccountBalance(@PathParam("accountNo") String accountNo, @Context HttpServletRequest request) {
        LOG.info("getAccountBalance");

        // Check that the user is logged in
        String response;

        if (!ServletUtil.isLoggedin(request)) {
            return Response.status(401).entity("{\"loggedIn\" : \"false\"}").build();
        }

        try {
            // Get the account balance
            double dblBalance = Account.getAccount(accountNo).getBalance();
            String format = (dblBalance < 1) ? "$0.00" : "$.00";
            String balance = new DecimalFormat(format).format(dblBalance);
            response = "{\"balance\" : \"" + balance + "\" ,\n";
        } catch (Exception e) {
            LOG.error(e.toString());
            return Response.status(500).entity("{Error : Unknown error occured during balance interogation}").build();
        }

        // Get the last 10 transactions
        String last10Transactions;
        last10Transactions = this.getLastTenTransactions(accountNo);
        if (last10Transactions.equals("Error")) {
            return Response.status(500).entity("{Error : Unexpected error during transfer interogation}").build();
        }
        response = response + last10Transactions;

        JSONObject myJson = new JSONObject();
        try {

            response = response + "\"credits\":[{\"account\":\"1001160140\", \"date\":\"2004-12-29\", \"description\":\"Paycheck\", \"amount\":\"1200\"},{\"account\":\"1001160140\", \"date\":\"2005-01-12\", \"description\":\"Paycheck\", \"amount\":\"1200\"},{\"account\":\"1001160140\", \"date\":\"2005-01-29\", \"description\":\"Paycheck\", \"amount\":\"1200\"},{\"account\":\"1001160140\", \"date\":\"2005-02-12\", \"description\":\"Paycheck\", \"amount\":\"1200\"},{\"account\":\"1001160140\", \"date\":\"2005-03-01\", \"description\":\"Paycheck\", \"amount\":\"1200\"},{\"account\":\"1001160140\", \"date\":\"2005-03-15\", \"description\":\"Paycheck\", \"amount\":\"1200\"}],";
            response = response + "\"debits\":[{\"account\":\"1001160140\", \"date\":\"2005-01-17\", \"description\": \"Withdrawal\" , \"amount\":\"2.85\"},{\"account\":\"1001160140\", \"date\":\"2005-01-25\", \"description\": \"Rent\" , \"amount\":\"800\"},{\"account\":\"1001160140\", \"date\":\"2005-01-27\", \"description\": \"Electric Bill\" , \"amount\":\"45.25\"},{\"account\":\"1001160140\", \"date\":\"2005-01-28\", \"description\": \"Heating\" , \"amount\":\"29.99\"},{\"account\":\"1001160140\", \"date\":\"2005-01-29\", \"description\": \"Transfer to Savings\" , \"amount\":\"321\"},{\"account\":\"1001160140\", \"date\":\"2005-01-29\", \"description\": \"Groceries\" , \"amount\":\"19.6\"}]}";
            myJson = new JSONObject(response);
            myJson.put("accountId", accountNo);
            return Response.status(200).entity(myJson.toString()).build();
        } catch (JSONException e) {
            LOG.error(e.toString());
        }
        return Response.status(200).entity("Standard" + response).build();
    }

    @GET
    @Path("/{accountNo}/transactions")
    @Produces("application/json")
    public Response showLastTenTransactions(@PathParam("accountNo") String accountNo, @Context HttpServletRequest request) {
        LOG.info("showLastTenTransactions");

        String response;

        if (!ServletUtil.isLoggedin(request)) {
            return Response.status(401).entity("{\"loggedIn\" : \"false\"}").build();
        }

        response = "{";
        // Get the last 10 transactions
        String last10Transactions;
        last10Transactions = this.getLastTenTransactions(accountNo);
        if (last10Transactions.equals("Error")) {
            return Response.status(500).entity("{Error : Unexpected error during transfer interogation}").build();
        }
        response = response + last10Transactions;
        response = response + "}";

        try {
            JSONObject myJson = new JSONObject();
            myJson = new JSONObject(response);
            return Response.status(200).entity(myJson.toString()).build();
        } catch (JSONException e) {
            LOG.error(e.toString());
            return Response.status(200).entity("{ \"Error\" : \"Unexpected error occured retrieving transactions.\"}").build();
        }
    }

    @POST
    @Path("/{accountNo}/transactions")
    @Produces("application/json")
    public Response getTransactions(@PathParam("accountNo") String accountNo, String bodyJSON, @Context HttpServletRequest request) {
        LOG.info("getTransactions");

        User user = ServletUtil.getUser(request);
        String startString;
        String endString;

        JSONObject myJson = new JSONObject();
        try {
            myJson = new JSONObject(bodyJSON);
            startString = (String) myJson.get("startDate");
            endString = (String) myJson.get("endDate");
        } catch (JSONException e) {
            LOG.error(e.toString());
            return Response.status(50).entity("{Error : Unexpected request format}").build();
        }

        Transaction[] transactions = new Transaction[0];

        try {
            Account[] account = new Account[1];
            account[0] = user.lookupAccount(Long.parseLong(accountNo));

            transactions = user.getUserTransactions(startString, endString, account);
        } catch (SQLException e) {
            LOG.error(e.toString());
            return Response.status(50).entity("{Error : Database failed to return requested data}").build();
        }

        String response = "{\"transactions\":[";

        for (int i = 0; i < transactions.length; i++) {
            // limit to 100 entries
            if (i == 100)
                break;

            double dblAmt = transactions[i].getAmount();
            String format = (dblAmt < 1) ? "$0.00" : "$.00";
            String amount = new DecimalFormat(format).format(dblAmt);
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(transactions[i].getDate());

            response += "{\"id\":" + "\"" + transactions[i].getTransactionId() + "\"," + "\"date\":" + "\"" + date + "\"," + "\"account\":\"" + transactions[i].getAccountId() + "\"," + "\"type\":\"" + transactions[i].getTransactionType() + "\"," + "\"amount\":\"" + amount + "\"}";
            if (i < transactions.length - 1)
                response += ",";
        }
        response += "]}";

        try {
            myJson = new JSONObject(response);
        } catch (JSONException e) {
            LOG.error(e.toString());
            return Response.status(200).entity("{\"error\" : \"" + e.getMessage() + "\"}\n").build();
        }
        return Response.status(200).entity(myJson.toString()).build();
    }

    private String getLastTenTransactions(String accountNo) {
        String response = "";
        try {
            response = response + "\"last_10_transactions\" :\n[";
            Transaction[] transactions = DBUtil.getTransactions(null, null, new Account[] { DBUtil.getAccount(Long.valueOf(accountNo)) }, 10);
            for (Transaction transaction : transactions) {
                double dblAmt = transaction.getAmount();
                String dollarFormat = (dblAmt < 1) ? "$0.00" : "$.00";
                String amount = new DecimalFormat(dollarFormat).format(dblAmt);
                String date = new SimpleDateFormat("yyyy-MM-dd").format(transaction.getDate());
                response = response + "{\"date\" : \"" + date + "\", \"transaction_type\" : \"" + transaction.getTransactionType() + "\", \"ammount\" : \"" + amount + "\" },\n";
            }
            response = response + "],\n";
        } catch (Exception e) {
            LOG.error(e.toString());
            return "Error";
        }

        return response;

    }

}
