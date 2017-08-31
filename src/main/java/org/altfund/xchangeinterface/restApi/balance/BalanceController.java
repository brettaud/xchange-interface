package org.altfund.xchangeinterface.restApi.balance;

import java.util.Map;
import org.altfund.xchangeinterface.xchange.model.EncryptedOrder;
import org.altfund.xchangeinterface.xchange.model.ExchangeCredentials;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.altfund.xchangeinterface.xchange.service.XChangeService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.altfund.xchangeinterface.util.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.altfund.xchangeinterface.xchange.service.OrderDecryptor;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

import lombok.extern.slf4j.Slf4j;

/*
 * The above example does not specify GET vs. PUT, POST, and so forth, because
 * @RequestMapping maps all HTTP operations by default. Use
 * @RequestMapping(method=GET) to narrow this mapping.
 */
@RestController
@Slf4j
public class BalanceController {
    private final XChangeService xChangeService;
    private final JsonHelper jh;
    private EncryptedOrder encryptedOrder;
    private OrderDecryptor orderDecryptor;
    private ExchangeCredentials exchangeCredentials;

    public BalanceController(XChangeService xChangeService, JsonHelper jh, OrderDecryptor orderDecryptor) {
        this.xChangeService = xChangeService;
        this.jh = jh;
        this.orderDecryptor = orderDecryptor;
    }

    @RequestMapping(value = "/balance", produces = "application/json")
    //public ResponseEntity<String> balance(@RequestParam String params) {
    //public ResponseEntity<String> balance(@RequestParam(value="params") String params) {
    public ResponseEntity<String> balance(@RequestParam Map<String, String> params) {
        String response = "";
        try {
            response = jh.getObjectMapper().writeValueAsString(params);
            log.debug("rec str {}.", response);
            encryptedOrder = jh.getObjectMapper().readValue(response, EncryptedOrder.class);
            log.debug("rec iv {}.", encryptedOrder.getIv());
            log.debug("rec data {}.", encryptedOrder.getEncryptedData());
            exchangeCredentials = jh.getObjectMapper().readValue( orderDecryptor.decrypt(encryptedOrder),
                                                                  ExchangeCredentials.class);
            //encrypted order needs to decrypt to a Map<String, String> :(
            //probs change that to a pojo like in ee.
            ObjectNode json = xChangeService.getExchangeBalances(exchangeCredentials);
            response = jh.getObjectMapper().writeValueAsString(json);
        }
        catch (IOException ex) {
            response = "{ERROR: IOException "+ ex.getMessage() + "}";
        }
        catch (NoSuchAlgorithmException ex) {
            response = "{ERROR: NoSuchAlgorithmException (error with decryption) "+ ex.getMessage() + "}";
        }
        catch (NoSuchPaddingException ex) {
            response = "{ERROR: NoSuchPaddingException (error with decryption) "+ ex.getMessage() + "}";
        }
        catch (InvalidKeyException ex) {
            response = "{ERROR: Invalid Key Exception (error with decryption) "+ ex.getMessage() + "}";
        }
        catch (IllegalBlockSizeException ex) {
            response = "{ERROR: Invalid Key Exception (error with decryption) "+ ex.getMessage() + "}";
        }
        catch (BadPaddingException ex) {
            response = "{ERROR: Invalid Key Exception (error with decryption) "+ ex.getMessage() + "}";
        }
        //catch (JsonProcessingException ex) {
        //    response = "{ERROR: JsonProcessingException "+ ex.getMessage() + "}";
        //}
        //return new BalanceMap(response.replace("\\", ""));
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(response, httpHeaders, HttpStatus.OK);
    }
}
