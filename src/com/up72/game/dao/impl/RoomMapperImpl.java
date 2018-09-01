package com.up72.game.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import com.up72.game.dao.RoomMapper;
import com.up72.server.mina.utils.MyBatisUtils;

/**
 * Created by admin on 2017/6/23.
 */
public class RoomMapperImpl implements RoomMapper {

    @Override
    public void insert(Map<String,String> entity) {
        System.out.println("保存房间信息");
        SqlSession session = MyBatisUtils.getSession();
        try {
            if (session != null) {
                String sqlName = RoomMapper.class.getName() + ".insert";
                session.insert(sqlName, entity);
                session.commit();
//                MyBatisUtils.closeSessionAndCommit();
            }
        } catch (Exception e) {
            System.out.println("insert room数据库操作出错！");
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateRoomState(Integer roomId,Integer xiaoJuNum) {
        SqlSession session = MyBatisUtils.getSession();
        try {
            if (session != null) {
                String sqlName = RoomMapper.class.getName() + ".updateRoomState";
                Map<Object, Object> map =new HashMap<>();
                map.put("roomId",roomId);
                map.put("xiaoJuNum", xiaoJuNum);
                session.update(sqlName,map);
                session.commit();
//                MyBatisUtils.closeSessionAndCommit();
            }
        } catch (Exception e) {
            System.out.println("数据库操作出错！");
        } finally {
            session.close();
        }
    }

}
