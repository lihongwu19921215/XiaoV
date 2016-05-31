/*
 * Copyright (c) 2012-2016, b3log.org & hacpai.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.xiaov.service;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.urlfetch.HTTPHeader;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.xiaov.util.XiaoVs;
import org.json.JSONObject;

/**
 * Turing query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, May 29, 2016
 * @since 1.0.0
 */
@Service
public class BaiduQueryService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(BaiduQueryService.class.getName());


    /**
     * Turing Robot Key.
     */
    private static final String BAIDU_COOKIE = XiaoVs.getString("baidu.cookie");
    
    
    private static final String QQ_BOT_NAME = XiaoVs.getString("qq.bot.name");
    /**
     * URL fetch service.
     */
    private static final URLFetchService URL_FETCH_SVC = URLFetchServiceFactory.getURLFetchService();
    
    private HTTPRequest request = null;
    /**
     * Chat with Turing Robot.
     *
     * @param userName the specified user name
     * @param msg the specified message
     * @return robot returned message, return {@code null} if not found
     */
    public String chat(String msg) {
    	String BAIDU_URL = "https://sp0.baidu.com/yLsHczq6KgQFm2e88IuM_a/s?sample_name=bear_brain&request_query=#MSG#&bear_type=2";
        if (StringUtils.isBlank(msg)) {
            return null;
        }
        LOGGER.info(msg);
    	if (msg.startsWith(QQ_BOT_NAME + "，")){
    		msg = msg.replace(QQ_BOT_NAME + "，", "");
    	}
    	if (msg.startsWith(QQ_BOT_NAME + ",")){
    		msg = msg.replace(QQ_BOT_NAME + ",", "");
    	}
    	if (msg.startsWith(QQ_BOT_NAME)){
    		msg = msg.replace(QQ_BOT_NAME, "");
    	}
    	LOGGER.info("百度：" + msg);
        request = new HTTPRequest();
        request.setRequestMethod(HTTPRequestMethod.POST);
        try {
        	
			BAIDU_URL = BAIDU_URL.replace("#MSG#", URLEncoder.encode(msg, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
        try {
        	HTTPHeader header = new HTTPHeader("Cookie", BAIDU_COOKIE);
        	request.addHeader(header);
            request.setURL(new URL(BAIDU_URL));
            HTTPResponse response = URL_FETCH_SVC.fetch(request);
            JSONObject data = new JSONObject(new String(response.getContent(), "UTF-8"));
            LOGGER.info(new String(response.getContent(), "UTF-8"));
            String content = (String) data.getJSONArray("result_list").getJSONObject(0).get("result_content");
            String answer = (String) new JSONObject(content).get("answer");
            answer = answer.replaceAll("小度", QQ_BOT_NAME);
            return answer;
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Chat with Turing Robot failed", e);
        }

        return null;
    }
}
