package com.mas.gov.bt.mas.primary.client;

import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Client for the external ECSS (Environmental Clearance System, ecss.systems.gov.bt) public
 * document endpoint. ECSS returns the EC document itself (a PDF) for a given EC number, guarded
 * by a static shared token rather than per-request auth.
 */
@Component
@Slf4j
public class EcssClient {

    private static final Pattern EC_NO_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]{1,50}$");

    private final RestTemplate restTemplate;

    @Value("${ecss.base-url}")
    private String ecssBaseUrl;

    @Value("${ecss.token}")
    private String ecssToken;

    public EcssClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] fetchEcDocument(String ecNo) {
        if (ecNo == null || !EC_NO_PATTERN.matcher(ecNo).matches()) {
            throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA, "Invalid EC number format");
        }

        URI url = URI.create(ecssBaseUrl + "/" + ecNo + "/?token=" + ecssToken);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            log.warn("ECSS returned non-success for EC number {}: {}", ecNo, response.getStatusCode());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "EC document could not be retrieved.");
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCodes.RESOURCE_NOT_FOUND, "No EC document found for " + ecNo);
            }
            log.error("ECSS API error for EC number {}: status={}", ecNo, e.getStatusCode());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "EC document could not be retrieved.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reach ECSS for EC number {}: {}", ecNo, e.getMessage());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "ECSS service is unavailable. Please try again.");
        }
    }
}
