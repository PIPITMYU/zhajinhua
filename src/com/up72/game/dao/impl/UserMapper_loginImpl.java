package com.up72.game.dao.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.alibaba.fastjson.JSONObject;
import com.up72.game.constant.Cnst;
import com.up72.game.dao.UserMapper_login;
import com.up72.game.dto.resp.Player;
import com.up72.server.mina.utils.MyLog;

/**
 * Created by admin on 2017/6/22.
 */
public class UserMapper_loginImpl implements UserMapper_login {
	private static MyLog log = MyLog.getLogger(UserMapper_loginImpl.class);
    @Override
    public Player getUserInfoByOpenId(String openId,String cid){
    	try {
    		String sendGet = sendGet(Cnst.USER_SERVICE_URL, "openId="+openId + "&cId="+cid);
//    		String sendGet = sendGet("http://192.168.1.156:8082/userService/returnUser", "openId="+openId + "&cId="+Cnst.cid);
    		if(sendGet == null)
        		return null;
        	JSONObject playerJson = JSONObject.parseObject(sendGet);

//        	JSONObject playerJson = parseObject;
        	
        	//TODO 先判断有没有这个玩家
        	Player p = new Player();
        	p.setUserId(playerJson.getLong("userId"));
        	p.setOpenId(openId);
        	p.setUserName(playerJson.getString("nickname"));
        	p.setUserImg(playerJson.getString("headimgurl"));
        	p.setGender(playerJson.getString("sex"));
//        	p.setGender("" + (new Random(System.currentTimeMillis()).nextInt(2) + 1));
        	return p;
		} catch (Exception e) {
			log.E("ERROR", e);
		}
    	
    	return null;
    }
    
    /**
     * 向指定URL发送GET方法的请求
     * 
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        StringBuilder result = new StringBuilder("");
        BufferedReader in = null;
        try {
        	String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(),"utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
            	result.append(line);
            }
        } catch (Exception e) {
           log.E("ERROR", e);
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
            	 log.E("ERROR", e2);
            }
        }
        return result.toString();
    }



}
