/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * 
 * WSO2.Telco Inc. licences this file to you under  the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.mediator.internal;

import org.wso2.carbon.user.core.service.RealmService;

// TODO: Auto-generated Javadoc
/**
 * The Class Util.
 */
public class Util {

	/** The realm service. */
	private static RealmService realmService;

	/**
	 * Gets the realm service.
	 *
	 * @return the realm service
	 */
	public static RealmService getRealmService() {
		return realmService;
	}

	/**
	 * Sets the realm service.
	 *
	 * @param realmSer
	 *            the new realm service
	 */
	public static synchronized void setRealmService(RealmService realmSer) {

		realmService = realmSer;

	}
	/**
	 * Checks if is all null.
	 *
	 * @param list
	 *            the list
	 * @return true, if is all null
	 */
	public static boolean isAllNull(Iterable<?> list) {
		for (Object obj : list) {
			if (obj != null)
				return false;
		}
		return true;
	}

}
