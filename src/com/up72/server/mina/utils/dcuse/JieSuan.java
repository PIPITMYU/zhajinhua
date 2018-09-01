package com.up72.server.mina.utils.dcuse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.up72.game.constant.Cnst;
import com.up72.game.dto.resp.Player;
import com.up72.game.dto.resp.RoomResp;
import com.up72.server.mina.function.GameFunctions;
import com.up72.server.mina.function.MessageFunctions;
import com.up72.server.mina.function.TCPGameFunctions;
import com.up72.server.mina.utils.RoomRecordUtil;
import com.up72.server.mina.utils.redis.RedisUtil;

public class JieSuan {
	/**
	 * 调用本方法前 请把所有的数据存入Redis
	 * 
	 * @param roomId
	 */
	public static void xiaoJieSuan(RoomResp room,String cid) {
		List<Player> players = RedisUtil.getPlayerList(room,cid);
		ArrayList<Long> arrayList = new ArrayList<Long>();
		

		for (int i = 0; i < room.getZhushu().size(); i++) {
			//如果出局了
			if(room.getChuju().contains(room.getPlayerIds().get(i)))
			{
				arrayList.add(0l - room.getZhushu().get(i));
			}
			else
			{
				room.setWinId(room.getPlayerIds().get(i));
				arrayList.add(0l - room.getZhushu().get(i) + room.getAll());
				room.getPlayerScores().set(i, room.getPlayerScores().get(i) + room.getAll());
			}
		}

		room.setXiaoJieSuanScore(arrayList);
		
		room.addXiaoJuFen(arrayList);
		
		room.setTotalNum(room.getTotalNum() + 1);
		// 
		for (Player p : players) {
			if(p == null)
				continue;
			p.setPlayStatus(Cnst.PLAYER_STATE_over);
		}
		
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			room.getPaiTypes().add(GameFunctions.getCardType(room, i));
		}
		
		room.setState(Cnst.ROOM_STATE_XJS);
		room.setPlayStatus(Cnst.ROOM_PLAYSTATE_NOTGAMING);
		
		
		RedisUtil.setPlayersList(players,cid);
		
		{
			Map<String,Object> map = new HashMap<String,Object>();
			map.put("interfaceId", "3");
			map.put("score", arrayList);
			map = GameFunctions.getNewMap(map);
			GameFunctions.addRecord(room, map);
		}
		
		if (room.getLastNum() == 0) {
			// 最后一局 大结算
			// room = RedisUtil.getRoomRespByRoomId(roomId);
			room.setState(Cnst.ROOM_STATE_XJS);

			RedisUtil.updateRedisData(room, null,cid);
			
			// 这里更新数据库吧
			TCPGameFunctions.updateDatabasePlayRecord(room,cid);
			MessageFunctions.interface_100140(room,cid);
			
			//数据记录保存到文件里面
			RoomRecordUtil.save(room.getRoomId(), Long.valueOf(room.getCreateTime()));
			
			if(room.getExtraType().intValue() == Cnst.ROOM_EXTRA_TYPE_2){
				RedisUtil.hdel(Cnst.DAI_KAI_KEY.concat(room.getCreateId() + ""), room.getRoomId() + "");
			}
		} else
			RedisUtil.updateRedisData(room, null,cid);
	}
}
