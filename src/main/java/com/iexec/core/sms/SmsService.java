package com.iexec.core.sms;

import com.iexec.common.utils.BytesUtils;
import com.iexec.core.feign.SmsClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class SmsService {

    private SmsClient smsClient;

    public SmsService(SmsClient smsClient) {
        this.smsClient = smsClient;
    }

    public String getEnclaveChallenge(String chainTaskId, boolean isTeeEnabled) {
        return isTeeEnabled
                ? generateEnclaveChallenge(chainTaskId)
                : BytesUtils.EMPTY_ADDRESS;
    }

    @Retryable(value = FeignException.class)
    public String generateEnclaveChallenge(String chainTaskId) {

        String teeChallengePublicKey = smsClient.generateTeeChallenge(chainTaskId);

        if (teeChallengePublicKey == null || teeChallengePublicKey.isEmpty()) {
            log.error("An error occured while getting teeChallengePublicKey [chainTaskId:{}]", chainTaskId);
            return "";
        }

        return teeChallengePublicKey;
    }

    @Recover
    public String generateEnclaveChallenge(FeignException e, String chainTaskId) {
        log.error("Failed to get enclaveChallenge from SMS even after retrying [chainTaskId:{}, attempts:3]", chainTaskId);
        e.printStackTrace();
        return "";
    }
}