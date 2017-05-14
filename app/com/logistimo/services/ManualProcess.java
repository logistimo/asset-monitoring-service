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

import com.logistimo.exception.ServiceException;
import com.logistimo.utils.LogistimoConstant;

import java.io.*;
import java.util.Date;

/**
 * Created by kaniyarasu on 22/06/16.
 */
public class ManualProcess {
    public static void main(String args[]){
        BufferedReader br = null;
        try {
            String sCurrentLine;
            String NEWLINE = "\n";
            br = new BufferedReader(new FileReader("/Users/kaniyarasu/Downloads/tmp/asset_status_json.txt"));
            System.out.println(new Date());
            int count = 0;

            File file = new File("/Users/kaniyarasu/Downloads/tmp/asset_status_json_final_440pm.txt");

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            StringBuilder content = new StringBuilder();
            while ((sCurrentLine = br.readLine()) != null) {
                content.append(PushAlertService.generatehmac(sCurrentLine)).append(LogistimoConstant.VENDOR_ID_PARAM).append(sCurrentLine).append(NEWLINE);
            }
            bw.write(content.toString());
            bw.close();

            System.out.println(PushAlertService.generatehmac("{\"data\":[{\"vId\":\"haier\",\"dId\":\"B2ABK0036\",\"st\":0,\"type\":3,\"time\":1466638515,\"tmp\":0,\"aSt\":0,\"mpId\":1},{\"vId\":\"haier\",\"dId\":\"B2ABK0036\",\"st\":0,\"type\":3,\"time\":1466638515,\"tmp\":0,\"aSt\":0,\"mpId\":2},{\"vId\":\"haier\",\"dId\":\"B2ABK0036\",\"st\":0,\"type\":3,\"time\":1466638515,\"tmp\":0,\"aSt\":0,\"mpId\":3},{\"vId\":\"haier\",\"dId\":\"B2ABK0036\",\"st\":0,\"type\":3,\"time\":1466638515,\"tmp\":0,\"aSt\":0,\"mpId\":4}]}"));
            System.out.println(new Date());
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}
