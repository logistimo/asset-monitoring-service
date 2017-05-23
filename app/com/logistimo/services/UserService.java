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

import com.logistimo.db.UserAccount;
import com.logistimo.models.user.request.AddApiConsumerRequest;
import com.logistimo.models.user.response.UserAccountResponse;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

public class UserService extends ServiceImpl {
  public UserAccountResponse getUserAccount(String userName) {
    return toUserAccountResponse(UserAccount.getUser(userName));
  }

  public List<UserAccountResponse> getAllUsers() {
    return toUserAccountResponseList(UserAccount.findAll());
  }

  private List<UserAccountResponse> toUserAccountResponseList(List<UserAccount> userAccountList) {
    List<UserAccountResponse>
        userResponseList =
        new ArrayList<UserAccountResponse>(userAccountList.size());
    for (UserAccount userAccount : userAccountList) {
      userResponseList.add(toUserAccountResponse(userAccount));
    }

    return userResponseList;
  }

  private UserAccountResponse toUserAccountResponse(UserAccount userAccount) {
    UserAccountResponse userAccountResponse = new UserAccountResponse();
    userAccountResponse.userName = userAccount.userName;
    userAccountResponse.password = userAccount.password;
    userAccountResponse.organizationName = userAccount.organizationName;
    userAccountResponse.userType = userAccount.userType;

    return userAccountResponse;
  }

  public void addUser(AddApiConsumerRequest addApiConsumerRequest) {
    toUserAccount(addApiConsumerRequest).save();
  }

  private UserAccount toUserAccount(AddApiConsumerRequest addApiConsumerRequest) {
    UserAccount userAccount = new UserAccount();

    userAccount.organizationName = addApiConsumerRequest.organizationName;
    userAccount.userName = addApiConsumerRequest.userName;
    userAccount.password = DigestUtils.md5Hex(addApiConsumerRequest.password);
    userAccount.userType = addApiConsumerRequest.userType;

    return userAccount;
  }
}
