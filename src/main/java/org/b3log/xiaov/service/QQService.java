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

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Strings;
import org.b3log.xiaov.util.XiaoVs;
import com.scienjus.smartqq.callback.MessageCallback;
import com.scienjus.smartqq.client.SmartQQClient;
import com.scienjus.smartqq.model.DiscussMessage;
import com.scienjus.smartqq.model.Group;
import com.scienjus.smartqq.model.GroupMessage;
import com.scienjus.smartqq.model.Message;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.commons.lang.math.RandomUtils;

/**
 * QQ service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.3.1.5, Jun 27, 2016
 * @since 1.0.0
 */
@Service
public class QQService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(QQService.class.getName());

    /**
     * QQ groups.
     *
     * &lt;groupId, group&gt;
     */
    private final Map<Long, Group> QQ_GROUPS = new HashMap<>();

    /**
     * The latest group ad time.
     *
     * &lt;groupId, time&gt;
     */
    private final Map<Long, Long> GROUP_AD_TIME = new HashMap<>();

    /**
     * QQ client.
     */
    private SmartQQClient xiaoV;

    /**
     * QQ client listener.
     */
    private SmartQQClient xiaoVListener;

    /**
     * Sent messages.
     */
    private List<String> GROUP_SENT_MSGS = new ArrayList<>();

    /**
     * Turing query service.
     */
    @Inject
    private TuringQueryService turingQueryService;

    /**
     * Baidu bot query service.
     */
    @Inject
    private BaiduQueryService baiduQueryService;

    /**
     * Bot type.
     */
    private static final int QQ_BOT_TYPE = XiaoVs.getInt("qq.bot.type");

    /**
     * Advertisements.
     */
    private static final List<String> ADS = new ArrayList<>();

    /**
     * URL fetch service.
     */
    private static final URLFetchService URL_FETCH_SVC = URLFetchServiceFactory.getURLFetchService();

    /**
     * XiaoV self intro. Built-in advertisement.
     */
    private static final String XIAO_V_INTRO = "你好，我是小薇机器人，加我（Q3082959578）和我的守护（Q316281008）为好友，然后将我们都邀请进群就可以开始聊天了~\nPS：小薇机器人使用问题请看帖 https://hacpai.com/article/1467011936362";

    /**
     * XiaoV listener self intro.
     */
    private static final String XIAO_V_LISTENER_INTRO = "你好，我是小薇机器人的守护，加我（Q316281008）和小薇机器人（Q3082959578）为好友，然后将我们都邀请进群就可以开始聊天了~\nPS：小薇机器人使用问题请看帖 https://hacpai.com/article/1467011936362";

    /**
     * No listener message.
     */
    private static final String NO_LISTENER = "请把我的守护（Q316281008）也拉进群，否则会造成大量消息重复（如果已经拉了，那就稍等 10 秒钟，我的守护可能在醒瞌睡 O(∩_∩)O哈哈~）\n\nPS：小薇机器人使用问题请看帖 https://hacpai.com/article/1467011936362";

    static {
        String adConf = XiaoVs.getString("ads");
        if (StringUtils.isNotBlank(adConf)) {
            final String[] ads = adConf.split("#");
            for (int i = 0; i < ads.length; i++) {
                ADS.add(ads[i]);
            }
        }

        ADS.add(XIAO_V_INTRO);
        ADS.add(XIAO_V_INTRO);
        ADS.add(XIAO_V_INTRO);
    }

    /**
     * Initializes QQ client.
     */
    public void initQQClient() {
        LOGGER.info("开始初始化小薇");

        xiaoV = new SmartQQClient(new MessageCallback() {
            @Override
            public void onMessage(final Message message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500 + RandomUtils.nextInt(2) * 1000);

                            final String content = message.getContent();
                            final String key = XiaoVs.getString("qq.bot.key");
                            if (!StringUtils.startsWith(content, key)) {
                                xiaoV.sendMessageToFriend(message.getUserId(), XIAO_V_INTRO);

                                return;
                            }

                            final String msg = StringUtils.substringAfter(content, key);
                            LOGGER.info("Received admin message: " + msg);
                            sendToQQGroups(msg);

                            Thread.sleep(1000 * 10);
                        } catch (final Exception e) {
                            LOGGER.log(Level.ERROR, "XiaoV on group message error", e);
                        }
                    }
                }).start();
            }

            @Override
            public void onGroupMessage(final GroupMessage message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500 + RandomUtils.nextInt(2) * 1000);

                            onQQGroupMessage(message);
                        } catch (final Exception e) {
                            LOGGER.log(Level.ERROR, "XiaoV on group message error", e);
                        }
                    }
                }).start();
            }

            @Override
            public void onDiscussMessage(final DiscussMessage message) {
            }
        });

        // Load groups
        reloadGroups();

        LOGGER.info("小薇初始化完毕，下面初始化小薇的守护（细节请看：https://github.com/b3log/xiaov/issues/3）");

        xiaoVListener = new SmartQQClient(new MessageCallback() {
            @Override
            public void onMessage(final Message message) {
                final String content = message.getContent();
                final String key = XiaoVs.getString("qq.bot.key");
                if (!StringUtils.startsWith(content, key)) {
                    xiaoV.sendMessageToFriend(message.getUserId(), XIAO_V_LISTENER_INTRO);

                    return;
                }

                final String msg = StringUtils.substringAfter(content, key);
                LOGGER.info("Received admin message: " + msg);
                sendToQQGroups(msg);
            }

            @Override
            public void onGroupMessage(final GroupMessage message) {
                final String content = message.getContent();
                if (GROUP_SENT_MSGS.contains(content)) { // indicates message received
                    GROUP_SENT_MSGS.remove(content);
                }
            }

            @Override
            public void onDiscussMessage(final DiscussMessage message) {
            }
        });

        LOGGER.info("小薇和小薇的守护初始化完毕！");
    }

    private void sendToForum(final String msg, final String user) {
        final String forumAPI = XiaoVs.getString("forum.api");
        final String forumKey = XiaoVs.getString("forum.key");

        final HTTPRequest request = new HTTPRequest();
        request.setRequestMethod(HTTPRequestMethod.POST);

        try {
            request.setURL(new URL(forumAPI));

            final String body = "key=" + URLEncoder.encode(forumKey, "UTF-8")
                    + "&msg=" + URLEncoder.encode(msg, "UTF-8")
                    + "&user=" + URLEncoder.encode(user, "UTF-8");
            request.setPayload(body.getBytes("UTF-8"));

            final HTTPResponse response = URL_FETCH_SVC.fetch(request);
            final int sc = response.getResponseCode();
            if (HttpServletResponse.SC_OK != sc) {
                LOGGER.warn("Sends message to Forum status code is [" + sc + "]");
            }
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Sends message to Forum failed: " + e.getMessage());
        }
    }

    /**
     * Closes QQ client.
     */
    public void closeQQClient() {
        if (null == xiaoV) {
            return;
        }

        try {
            xiaoV.close();
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Closes QQ client failed", e);
        }
    }

    /**
     * Sends the specified article to QQ groups.
     *
     * @param msg the specified message
     */
    public void sendToPushQQGroups(final String msg) {
        try {
            final String pushGroupsConf = XiaoVs.getString("qq.bot.pushGroups");
            if (StringUtils.isBlank(pushGroupsConf)) {
                return;
            }

            if (StringUtils.equals(pushGroupsConf, "*")) {
                // Push to all groups
                for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
                    final Group group = entry.getValue();

                    xiaoV.sendMessageToGroup(group.getId(), msg); // Without retry

                    Thread.sleep(1000 * 10);
                }

                return;
            }

            // Push to the specified groups
            final String[] groups = pushGroupsConf.split(",");
            for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
                final Group group = entry.getValue();
                final String name = group.getName();

                if (Strings.contains(name, groups)) {
                    xiaoV.sendMessageToGroup(group.getId(), msg); // Without retry

                    Thread.sleep(1000 * 10);
                }
            }
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Push message [" + msg + "] to groups failed", e);
        }
    }

    private void sendToQQGroups(final String msg) {
        for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
            final Group group = entry.getValue();
            sendMessageToGroup(group.getId(), msg);
        }
    }

    private void sendMessageToGroup(final Long groupId, final String msg) {
        Group group = QQ_GROUPS.get(groupId);
        if (null == group) {
            reloadGroups();

            group = QQ_GROUPS.get(groupId);
        }

        if (null == group) {
            LOGGER.log(Level.ERROR, "Group list error [groupId=" + groupId + "], please report this bug to developer: https://github.com/b3log/xiaov/issues/new");

            return;
        }

        LOGGER.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "]");
        xiaoV.sendMessageToGroup(groupId, msg);

        GROUP_SENT_MSGS.add(msg);

        if (GROUP_SENT_MSGS.size() > QQ_GROUPS.size() * 5) {
            GROUP_SENT_MSGS.remove(0);
        }

        final int maxRetries = 3;
        int retries = 0;
        int sentTries = 0;
        while (retries < maxRetries) {
            retries++;

            try {
                Thread.sleep(3500);
            } catch (final Exception e) {
                continue;
            }

            if (GROUP_SENT_MSGS.contains(msg)) {
                LOGGER.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "] with retries [" + retries + "]");
                xiaoV.sendMessageToGroup(groupId, msg);
                sentTries++;
            }
        }

        if (maxRetries == sentTries) {
            LOGGER.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "] with retries [" + retries + "]");
            xiaoV.sendMessageToGroup(groupId, NO_LISTENER);
        }
    }

    public void onQQGroupMessage(final GroupMessage message) {
        final long groupId = message.getGroupId();

        final String content = message.getContent();
        final String userName = Long.toHexString(message.getUserId());
        // Push to forum
        String qqMsg = content.replaceAll("\\[\"face\",[0-9]+\\]", "");
        if (StringUtils.isNotBlank(qqMsg)) {
            qqMsg = "<p>" + qqMsg + "</p>";
            sendToForum(qqMsg, userName);
        }

        String msg = "";
        if (StringUtils.contains(content, XiaoVs.getString("qq.bot.name"))
                || (StringUtils.length(content) > 6
                && (StringUtils.contains(content, "?") || StringUtils.contains(content, "？") || StringUtils.contains(content, "问")))) {
            msg = answer(content, userName);
        }

        if (StringUtils.isBlank(msg)) {
            return;
        }

        if (RandomUtils.nextFloat() >= 0.9) {
            final Long latestAdTime = GROUP_AD_TIME.get(groupId);
            final long now = System.currentTimeMillis();

            if (now - latestAdTime > 1000 * 60 * 30) {
                msg = msg + "\n\n（" + ADS.get(RandomUtils.nextInt(ADS.size())) + ")";

                GROUP_AD_TIME.put(groupId, now);
            }
        }

        sendMessageToGroup(groupId, msg);
    }

    private String answer(final String content, final String userName) {
        String keyword = "";
        String[] keywords = StringUtils.split(XiaoVs.getString("bot.follow.keywords"), ",");
        keywords = Strings.trimAll(keywords);
        for (final String kw : keywords) {
            if (StringUtils.containsIgnoreCase(content, kw)) {
                keyword = kw;

                break;
            }
        }

        String ret = "";
        if (StringUtils.isNotBlank(keyword)) {
            try {
                ret = XiaoVs.getString("bot.follow.keywordAnswer");
                ret = StringUtils.replace(ret, "{keyword}",
                        URLEncoder.encode(keyword, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                LOGGER.log(Level.ERROR, "Search key encoding failed", e);
            }
        } else if (StringUtils.contains(content, XiaoVs.QQ_BOT_NAME)) {
            if (1 == QQ_BOT_TYPE) {
                ret = turingQueryService.chat(userName, content);
                ret = StringUtils.replace(ret, "图灵机器人", "小薇机器人");
                ret = StringUtils.replace(ret, "<br>", "\n");
            } else if (2 == QQ_BOT_TYPE) {
                ret = baiduQueryService.chat(content);
            }

            if (StringUtils.isBlank(ret)) {
                ret = "嗯~";
            }
        }

        return ret;
    }

    private void reloadGroups() {
        final List<Group> groups = xiaoV.getGroupList();
        QQ_GROUPS.clear();
        GROUP_AD_TIME.clear();

        final StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("Reloaded groups: \n");
        for (final Group g : groups) {
            QQ_GROUPS.put(g.getId(), g);
            GROUP_AD_TIME.put(g.getId(), 0L);

            msgBuilder.append("    ").append(g.getName()).append(": ").append(g.getId()).append("\n");
        }

        LOGGER.log(Level.INFO, msgBuilder.toString());
    }
}
