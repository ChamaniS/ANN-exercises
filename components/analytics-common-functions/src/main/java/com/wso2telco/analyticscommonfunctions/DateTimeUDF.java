/**
 * Copyright (c) 2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wso2telco.analyticscommonfunctions;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;



public class DateTimeUDF {

    private final Calendar cal = Calendar.getInstance();
    
    public Integer getYear(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.YEAR);
        }
    }

    public Integer getMonth(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.MONTH) + 1;
        }
    }

    public Integer getDay(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.DAY_OF_MONTH);
        }
    }

    public Integer getHour(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.HOUR_OF_DAY);
        }
    }

    public Integer getMinute(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.MINUTE);
        }
    }

    public Integer getSeconds(Long timeStamp) throws ParseException {
        synchronized (cal) {
            cal.setTimeInMillis(timeStamp);
            return cal.get(Calendar.SECOND);
        }
    }

    public String getMonthStartingTime(Integer year, Integer month) {
        synchronized (cal) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return String.valueOf(cal.getTimeInMillis());
        }
    }

    public String getDateStartingTime(Integer year, Integer month, Integer date) {
        synchronized (cal) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return String.valueOf(cal.getTimeInMillis());
        }
    }

    public String getHourStartingTime(Integer year, Integer month, Integer date, Integer hour) {
        synchronized (cal) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, date);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return String.valueOf(cal.getTimeInMillis());
        }
    }

    public String getMinuteStartingTime(Integer year, Integer month, Integer date, Integer hour, Integer minute) {
        synchronized (cal) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, date);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return String.valueOf(cal.getTimeInMillis());
        }
    }

    public String getSecondStartingTime(Integer year, Integer month, Integer date, Integer hour, Integer minute,
                                        Integer second) {
        synchronized (cal) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, date);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, 0);
            return String.valueOf(cal.getTimeInMillis());
        }
    }
    
    public String getFormattedDate(Long time, String format){
    	Timestamp timeStamp = new Timestamp(time);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(timeStamp);

    }
}