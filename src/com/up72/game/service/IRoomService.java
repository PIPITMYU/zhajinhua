/*
 * Powered By [up72-framework]
 * Web Site: http://www.up72.com
 * Since 2006 - 2017
 */

package com.up72.game.service;
import java.util.Map;


/**
 * 接口
 * 
 * @author up72
 * @version 1.0
 * @since 1.0
 */
public interface IRoomService {

    void save(Map<String,String> room);

    void updateRoomState(Integer roomId,Integer xiaoJuNum);

}
