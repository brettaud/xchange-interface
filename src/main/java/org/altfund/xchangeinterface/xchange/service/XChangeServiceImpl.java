package org.altfund.xchangeinterface.xchange.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Optional;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dozer.DozerBeanMapper;

import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsAll;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;

import org.altfund.xchangeinterface.xchange.model.Order;
import org.altfund.xchangeinterface.xchange.model.MarketByExchanges;
import org.altfund.xchangeinterface.xchange.model.OrderSpec;
import org.altfund.xchangeinterface.xchange.model.OrderResponse;
import org.altfund.xchangeinterface.xchange.model.Exchange;
import org.altfund.xchangeinterface.xchange.model.ExchangeCredentials;
import org.altfund.xchangeinterface.xchange.model.TradeHistory;
import org.altfund.xchangeinterface.xchange.model.TradeHistoryParams;
import org.altfund.xchangeinterface.xchange.model.OpenOrder;
import org.altfund.xchangeinterface.util.JsonHelper;
import org.altfund.xchangeinterface.util.KWayMerge;
import org.altfund.xchangeinterface.xchange.service.util.ExtractCurrencies;
import org.altfund.xchangeinterface.xchange.service.util.ExtractExchangeTickers;
import org.altfund.xchangeinterface.xchange.service.util.ExtractOrderBooks;
import org.altfund.xchangeinterface.xchange.service.util.ExtractTradeFees;
import org.altfund.xchangeinterface.xchange.service.util.ExtractBalances;
//import org.altfund.xchangeinterface.xchange.service.util.ExtractUserTrades;
import org.altfund.xchangeinterface.xchange.service.util.LimitOrderPlacer;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.altfund.xchangeinterface.xchange.service.exceptions.XChangeServiceException;

/**
 * altfund
 */
@Slf4j
public class XChangeServiceImpl implements XChangeService {

    private final XChangeFactory xChangeFactory;
    private final JsonHelper jh;
    private final KWayMerge kWayMerge;
    private final LimitOrderPlacer limitOrderPlacer;
    private final DozerBeanMapper dozerBeanMapper;

    public XChangeServiceImpl(XChangeFactory xChangeFactory,
                              JsonHelper jh,
                              LimitOrderPlacer limitOrderPlacer,
                              DozerBeanMapper dozerBeanMapper,
                              KWayMerge kWayMerge) {
        this.xChangeFactory = xChangeFactory;
        this.jh = jh;
        this.limitOrderPlacer = limitOrderPlacer;
        this.dozerBeanMapper = dozerBeanMapper;
        this.kWayMerge = kWayMerge;
    }

    @Override
    public ObjectNode getExchangeCurrencies(String exchange) {
        Optional<ExchangeMetaData>  metaData;
        ObjectNode currencyMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        try {
            //xChangeFactory.setProperties(exchange);
            metaData = Optional.ofNullable(xChangeFactory.getExchangeMetaData(exchange));
            if (!metaData.isPresent()){
                errorMap.put("ERROR", "No such exchange " + exchange);
                return errorMap;
            }

            currencyMap =  ExtractCurrencies.toJson(metaData.get().getCurrencies(), exchange, jh);
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return currencyMap;
    }

    public ObjectNode getTickers(String exchange) {
        Optional<List<CurrencyPair>>  currencyPairs;
        Optional<MarketDataService>  marketDataService;
        ObjectNode tickerMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        try {
            //xChangeFactory.setProperties(exchange);
            currencyPairs = Optional.ofNullable(xChangeFactory.getExchangeSymbols(exchange));
            if (!currencyPairs.isPresent()){
                errorMap.put("ERROR", "No such exchange " + exchange);
                return errorMap;
            }

            marketDataService = Optional.ofNullable(xChangeFactory.getMarketDataService(exchange));
            if (!marketDataService.isPresent()){
                errorMap.put("ERROR", "No such exchange " + exchange);
                return errorMap;
            }

            tickerMap =  ExtractExchangeTickers.toJson(currencyPairs.get(), marketDataService.get(), exchange, jh);
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return tickerMap;
    }

    public ObjectNode getOrderBooks(Map<String, String> params) {
        Optional<MarketDataService>  marketDataService;
        ObjectNode orderBookMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        try {
            //xChangeFactory.setProperties(params.get("exchange"));
            marketDataService = Optional.ofNullable(xChangeFactory.getMarketDataService(params.get( "exchange" )));
            if (!marketDataService.isPresent()){
                errorMap.put("ERROR", "No such exchange " + params.get( "exchange" ));
                return errorMap;
            }

            //params for this method are needed because it has "base_currency" and "quote_currency"
            orderBookMap =  ExtractOrderBooks.toJson(marketDataService.get(), params, jh);
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return orderBookMap;
    }

    @Override
    public ObjectNode getAggregateOrderBooks(MarketByExchanges marketByExchanges) {
        List<OrderBook> books = null;
        MarketDataService marketDataService = null;
        List<String> exchanges = marketByExchanges.getExchanges();
        ArrayList<List<LimitOrder>> asks = new ArrayList<List<LimitOrder>>();
        ArrayList<List<LimitOrder>> bids = new ArrayList<List<LimitOrder>>();
        List<LimitOrder> aggregatedAsks = new ArrayList<LimitOrder>();
        List<LimitOrder> aggregatedBids = new ArrayList<LimitOrder>();
        OrderBook ob = null;
        ObjectNode orderBookMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        CurrencyPair cp = new CurrencyPair(
                marketByExchanges.getQuoteCurrency(),
                marketByExchanges.getBaseCurrency());

        try {
            log.debug("Begin extract MarketDataService(s)");
            for (int i = 0; i < exchanges.size(); i++) {
                log.debug("Get MarketDataService for {}", exchanges.get(i));
                marketDataService = xChangeFactory.getMarketDataService(exchanges.get(i));
                ob = ExtractOrderBooks.raw(marketDataService, cp, exchanges.get(i));

                asks.add(ob.getAsks());
                bids.add(ob.getBids());
            }
            aggregatedAsks = kWayMerge.mergeKLists(asks);
            aggregatedBids = kWayMerge.mergeKLists(bids);
            orderBookMap.put("ASKS", jh.getObjectMapper().writeValueAsString(aggregatedAsks));
            orderBookMap.put("BIDS", jh.getObjectMapper().writeValueAsString(aggregatedBids));
            //params for this method are needed because it has "base_currency" and "quote_currency"
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            //log.error("xchangeservice excepti)on SHOULD THROW TO CALLER \n{}", ex.getStackTrace());
            log.error("{}", ex.getStackTrace());
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (Exception ex) {
            // import java.time.LocalDateTime;
            //log.error("xchangeservice exception SHOULD THROW TO CALLER \n{}", ex.getStackTrace());
            log.error("{}", ex.getStackTrace());
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return orderBookMap;
    }

    /*
    public ObjectNode getAggregatedOrderBooks(Map<String, String> params) {
        MarketDataService  marketDataService;
        OrderBook orderBook = null;
        ObjectNode errorMap = jh.getObjectNode();

        try {
            //xChangeFactory.setProperties(params.get("exchange"));
            //marketDataService = Optional.ofNullable(xChangeFactory.getMarketDataService(params.get( "exchange" )));
            marketDataService = xChangeFactory.getMarketDataService(params.get("exchange"));

            //params for this method are needed because it has "base_currency" and "quote_currency"
            orderBookMap =  ExtractOrderBooks.raw(marketDataService, params, jh);
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return orderBookMap;
    }
    */

    @Override
    public ObjectNode getExchangeTradeFees(Map<String, String> params) {
        Optional<ExchangeMetaData>  metaData;
        ObjectNode tradeMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        try {
            //xChangeFactory.setProperties(params.get("exchange"));
            metaData = Optional.ofNullable(xChangeFactory.getExchangeMetaData(params.get("exchange")));
            if (!metaData.isPresent()){
                errorMap.put("ERROR", "No such exchange " + params.get("exchange"));
                return errorMap;
            }

            tradeMap =  ExtractTradeFees.toJson(metaData.get().getCurrencyPairs(), params.get("exchange"), jh);
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        return tradeMap;
    }

    @Override
    public ObjectNode getExchangeBalances(ExchangeCredentials exchangeCredentials) {
        Optional<AccountService> accountService;
        Optional<AccountInfo> accountInfo;
        Optional<Map<String, Wallet>> wallets;
        Optional<BigDecimal> tradingFee;
        ObjectNode balanceMap = jh.getObjectNode();
        ObjectNode errorMap = jh.getObjectNode();

        try {
            accountService = Optional.ofNullable(xChangeFactory.getAccountService(exchangeCredentials));
            if (!accountService.isPresent()){
                errorMap.put("ERROR", exchangeCredentials.getExchange() + "No such account service");
                return errorMap;
            }

            try {
                accountInfo = Optional.ofNullable(accountService.get().getAccountInfo());
                if (!accountInfo.isPresent()){
                    errorMap.put("ERROR", exchangeCredentials.getExchange() + "No such account info");
                    return errorMap;
                }
            } catch (Exception ex) {
                errorMap.put("ERROR", exchangeCredentials.getExchange() + ex.toString() + ": " + ex.getMessage());
                return errorMap;
            }

            wallets = Optional.ofNullable(accountInfo.get().getWallets());
            if (!wallets.isPresent()){
                errorMap.put("ERROR", exchangeCredentials.getExchange() + "No such wallets");
                return errorMap;
            }

            balanceMap = ExtractBalances.toJson(wallets.get(), exchangeCredentials.getExchange(), jh);
        }
        catch (XChangeServiceException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", exchangeCredentials.getExchange() + ex.toString() + ": " + ex.getMessage());
            return errorMap;
        }
        catch (IOException ex) {
            // import java.time.LocalDateTime;
            errorMap.put("ERROR", ex.getMessage());
            return errorMap;
        }
        log.debug("balancemap " + balanceMap);
        return balanceMap;
    }

    @Override
    public boolean cancelLimitOrder(Order order) throws Exception {
        ExchangeCredentials exchangeCredentials = null;
        ObjectNode errorMap = jh.getObjectNode();
        TradeService tradeService = null;
        exchangeCredentials = order.getExchangeCredentials();
        boolean orderResponse = false;

        try {
            tradeService = xChangeFactory.getTradeService(exchangeCredentials);
            orderResponse = tradeService.cancelOrder(order.getOrderId());
        }
        catch (Exception e) {
            throw e;
        }
        /*
        catch (IOException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (XChangeServiceException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (RuntimeException re) {
            log.error("Non-retyable error {}: " + re, re.getMessage());
        }
        */
        return orderResponse;
    }

    @Override
    public OrderResponse placeLimitOrder(Order order) throws Exception{
        OrderSpec orderSpec = null;
        ExchangeCredentials exchangeCredentials = null;
        OrderResponse orderResponse = null;
        ObjectNode errorMap = jh.getObjectNode();
        TradeService tradeService = null;
        CurrencyPair currencyPair = null;
        int scale = 5;
        exchangeCredentials = order.getExchangeCredentials();
        String orderType = order.getOrderType();

        if (!"ASK".equals(orderType.toUpperCase()) || !"BID".equals(orderType.toUpperCase())) {
            //errorMap.put("ERROR", "order type MUST be equal to 'ASK' or 'BID'");
            //return errorMap;
            log.error("wrong value, must be ASK or BID");
            //TODO throw XChangeServiceException;
        }

        try {
            tradeService = xChangeFactory.getTradeService(exchangeCredentials);
            currencyPair = new CurrencyPair(
                order.getOrderSpec().getBaseCurrency().name(),
                order.getOrderSpec().getQuoteCurrency().name()
            );

            //scale = xChangeFactory.getExchangeScale(order.getExchangeCredentials(), currencyPair);

            orderResponse = limitOrderPlacer.placeOrder(order, tradeService, currencyPair, scale, jh);
            /*
            if (orderResponse.isRetryable()) {
                //TODO is using same orderResponse wrong?
                orderResponse = limitOrderPlacer.placeOrder(order, tradeService, currencyPair, scale, jh);
            }
            */
        }
        /*
        catch (IOException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (XChangeServiceException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (NoSuchElementException e) {
            log.error("NoSuchElementException {}: " + e, e.getMessage());
        }
        catch (RuntimeException re) {
            log.error("Non-retyable error {}: " + re, re.getMessage());
        }
        */
        catch (Exception e){
            throw e;
        }
        log.debug("Order Response returning");
        return orderResponse;
    }

    @Override
    public String getTradeHistory(TradeHistory tradeHistory) throws Exception {
        ExchangeCredentials exchangeCredentials = null;
        TradeService tradeService = null;
        CurrencyPair currencyPair = null;
        TradeHistoryParams tradeHistoryParams = null;
        int scale = 5;
        exchangeCredentials = tradeHistory.getExchangeCredentials();
        tradeHistoryParams = tradeHistory.getTradeHistoryParams();
        TradeHistoryParamsAll tradeParams = null;
        UserTrades userTrades = null;
        String response = "";

        log.debug("Begin retrieve trade history");
        try {
            tradeService = xChangeFactory.getTradeService(exchangeCredentials);

            log.debug("Call to Dozer...");
            tradeParams = dozerBeanMapper.map(tradeHistoryParams, TradeHistoryParamsAll.class);
            log.debug("Dozer call success.");

            userTrades = tradeService.getTradeHistory(tradeParams);

            response = jh.getObjectMapper().writeValueAsString(userTrades);

        }
        catch (Exception ex) {
            log.debug("{}: {}", ex.getMessage());
            throw ex;
        }
        return response;
    }

    @Override
    public String getOpenOrders(ExchangeCredentials exchangeCredentials) throws Exception {
        //ExchangeCredentials exchangeCredentials = null;
        //exchangeCredentials = openOrder.getExchangeCredentials();

        org.knowm.xchange.service.trade.params.orders.OpenOrdersParams knowmOpenOrderParms = null;
        //knowmOpenOrderParms = openOrder.getOpenOrderParams();

        List<LimitOrder> openOrders = null;
        ObjectNode errorMap = jh.getObjectNode();
        //ObjectNode userTradesMap = jh.getObjectNode();
        TradeService tradeService = null;
        CurrencyPair currencyPair = null;
        int scale = 5;
        UserTrades userTrades = null;
        String response = "";

        try {
            tradeService = xChangeFactory.getTradeService(exchangeCredentials);
            knowmOpenOrderParms = tradeService.createOpenOrdersParams();

            /*
            CurrencyPair cp = new CurrencyPair(
                    openOrder.getOpenOrderParams().getBaseCurrency(),
                    openOrder.getOpenOrderParams().getQuoteCurrency()
                    );
            */
            //knowmOpenOrderParms.setCurrencyPair(cp);


            /*
            knowmOpenOrderParms = dozerBeanMapper.map(
                                        openOrder.getOpenOrderParams(),
                                        org.knowm.xchange.service.trade.params.orders.OpenOrdersParams.class);
            openOrders = tradeService.getOpenOrders(knowmOpenOrderParms).getOpenOrders();
            */

            //openOrders = tradeService.getOpenOrders(knowmOpenOrderParms).getOpenOrders();
            response = tradeService.getOpenOrders(knowmOpenOrderParms).toString();
            log.debug("OPEN ORDERS: {}", response);

            //userTradesMap = ExtractUserTrades.toJson(userTrades, exchangeCredentials.getExchange(), jh);
            //response = jh.getObjectMapper().writeValueAsString(openOrders);

        }
        catch (Exception e) {
            throw e;
        }
        /*
        //TODO return errors as json
        catch (IOException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (XChangeServiceException e) {
            log.error("XChangeServiceException {}: " + e, e.getMessage());
        }
        catch (RuntimeException re) {
            log.error("Non-retyable error {}: " + re, re.getMessage());
        }
        */
        return response;
    }
}
