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
import org.altfund.xchangeinterface.xchange.service.MessageEncryption;
import org.altfund.xchangeinterface.api.balance.BalanceEndpoint;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

import lombok.extern.slf4j.Slf4j;
import org.altfund.xchangeinterface.restApi.util.ResponseHandler;

/*
 * The above example does not specify GET vs. PUT, POST, and so forth, because
 * @RequestMapping maps all HTTP operations by default. Use
 * @RequestMapping(method=GET) to narrow this mapping.
 */
@RestController
@Slf4j
public class BalanceController {
    private EncryptedOrder encryptedOrder;
    private final MessageEncryption messageEncryption;
    /*
    private final XChangeService xChangeService;
    private ExchangeCredentials exchangeCredentials;
    */
    private final JsonHelper jh;
    private final ResponseHandler rh;
    private final BalanceEndpoint balance;

    //public BalanceController(XChangeService xChangeService, JsonHelper jh, MessageEncryption messageEncryption, ResponseHandler rh) {
    public BalanceController(BalanceEndpoint balance, ResponseHandler rh, MessageEncryption messageEncryption, JsonHelper jh) {
        this.balance = balance;
        this.rh = rh;
        this.messageEncryption = messageEncryption;
        this.jh = jh;
        /*
        this.xChangeService = xChangeService;
        */
    }

    @RequestMapping(value = "/balance", produces = "application/json")
    public ResponseEntity<String> balance(@RequestParam Map<String, String> params) {
        String response = "";
        try {
            response = jh.getObjectMapper().writeValueAsString(params);
            log.debug("rec str {}.", response);
            encryptedOrder = jh.getObjectMapper().readValue(response, EncryptedOrder.class);
            log.debug("rec iv {}.", encryptedOrder.getIv());
            log.debug("rec data {}.", encryptedOrder.getEncryptedData());


            response = balance.getBalance(messageEncryption.decrypt(encryptedOrder));
        }
        catch (Exception ex) {
            return rh.send(ex, true);
        }
        return rh.send(response, true);
    }
}
