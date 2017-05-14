/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.services;

import com.logistimo.utils.LogistimoUtils;
import org.apache.commons.codec.binary.Base64;
import play.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Created by kaniyarasu on 12/08/15.
 */
public class PushAlertService extends ServiceImpl implements Executable {

    private static final Logger.ALogger LOGGER = Logger.of(PushAlertService.class);
    private final static String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String PRODUCER_ID = "ASSET-ALARMS";

    private static MessagingService messagingService = ServiceFactory.getService(MessagingService.class);

    @Override
    public void process(String content, Map<String, Object> options) throws Exception {
        messagingService.produceMessage(PRODUCER_ID, content);
    }

    public static String generatehmac(String data) {
        return hmac("X0FIbIBUHmTNV0SpYdTfJsISTUMnM59UDSTfrbcTTk8iMRh4wpjHf99fXYDotlt5", data);
    }

    /**
     * Creates Hmac string from application secret and post data
     *
     * @param secret
     * @param data
     * @return
     */
    private static String hmac(String secret, String data) {
        try {
            SecretKeySpec signatureKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA1_ALGORITHM);
            Mac m = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            m.init(signatureKey);
            byte[] rawHmac = m.doFinal(data.getBytes());
            String result = new String(Base64.encodeBase64(rawHmac));
            return result;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Unexpected error while creating hmac", e);
            throw new IllegalArgumentException();
        }
    }
}
